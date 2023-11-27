package projeto_ufu.fd;

import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;

/**
 * 15/09/2014
 *
 * @author Anubis
 */
public class FDImpact {

    protected class DeviceReply {
        protected long mid;
        protected long arrivalTime;

        protected DeviceReply(long mid, long arrivalTime) {
            this.mid = mid;
            this.arrivalTime = arrivalTime;
        }
    }

    private int sizeList = 0;// size window
    private final Queue<DeviceReply>[] A; // queue
    private final long Ai; // nanoseconds
    private long EA = 0;
    private int[] set;
    private final int server;
    private final int threshold;
    private final long margin;
    private final int nset;
    private final String trace;
    private final boolean[] trusted = { true, true, true, true, true, true, true, true, true, true, };

    private final DescriptiveStatistics samplesDT; // used for statistics

    public FDImpact(int[] set, int threshold, int nset,
            long margin, String trace, int sizeList, int server, long Ai) {
        samplesDT = new DescriptiveStatistics(45140000); // statistics of detection time
        this.sizeList = sizeList;
        this.set = set;
        this.nset = nset;
        this.margin = margin;
        this.threshold = threshold;
        this.trace = trace;
        this.server = server;
        this.A = new Queue[this.set.length];
        this.Ai = Ai;
        for (int j = 0; j < this.A.length; j++) {
            this.A[j] = new LinkedList();
        }
    }

    public void execute() {
        OutputStream os;
        BufferedWriter bw;
        FileInputStream inputStream;
        OutputStreamWriter osw;

        Scanner sc;
        String[] stringArray;

        long trustLevel;
        long error = 0, tEnd = 0, timeoutPrev = 0, timeoutIni;
        long errorIni = 0, mistakeTime = 0, dif;
        long[] to, tPrevious; // to ->timeout , tPrevious-> previous time
        long ts = 0, tsIni = 0, totTime = 0; // ts -> timestamp / tsIni -> initial timestamp / totTime -> total time
        double rate, pa;
        boolean untrusted = false;
        int id, lin = 1;
        int listSize;
        String line;

        to = new long[10]; // timeout
        tPrevious = new long[10]; // previous timeout

        for (int j = 0; j < 10; j++) {
            to[j] = 0;
        }

        NumberFormat f = new DecimalFormat("0.000000000000000");

        try {
            inputStream = new FileInputStream(trace);
            sc = new Scanner(inputStream, "UTF-8");
            while (sc.hasNextLine()) {
                line = sc.nextLine();
                stringArray = line.split(" ");
                id = Integer.valueOf(stringArray[0]);
                int mid = Integer.valueOf(stringArray[1]);
                ts = Long.valueOf(stringArray[3]); // arrival timestamp

                if (lin == 1) {
                    tsIni = ts; // initial timestamp
                }
                listSize = A[id].size();
                if (listSize > 0) {
                    timeoutIni = to[id];
                } else { // if list is empty
                    if (lin == 1) { // if first line
                        timeoutIni = ts; // timeoutIni ->
                    } else {
                        timeoutIni = timeoutPrev; // previous timeout
                    }
                }
                /*
                 * loop to check if the timeout of each of the nodes is expired
                 */
                for (int j = 0; j < 10; j++) {
                    /*
                     * if the impact factor is different from 0
                     *                      and estimated timeout is different from 0
                     *                      and the node is different from the current one
                     */
                    if (set[j] != 0 && (to[j] != 0) && j != id && trusted[j]) {
                        /*
                         * if the current timestamp is greater than the estimated timeout of j
                         *                          and estimated timeout of j is greater than the last
                         * timeout (if not already computed before)
                         *                          and j is trusted
                         */
                        if ((ts > to[j]) && (to[j] > tEnd)) {
                            // in this case j is considered suspicious and leaves the trusted list
                            trusted[j] = false;
                            System.out.println("Device " + j + " is NOT TRUSTED");
                            trustLevel = sumTrusted(); //// calculate the trust_level
                            if (trustLevel < this.threshold) {
                                if (to[j] < timeoutIni) {
                                    timeoutIni = to[j]; // the timeout of j is the initial error time
                                }
                            }
                        }
                    }
                }
                trusted[id] = true; // the node is trusted because a heartbeat arrived
                trustLevel = sumTrusted(); // calculate the trust level
                if (trustLevel < this.threshold) { // unstrusted
                    if (!untrusted) { // if before the system was trusted
                        errorIni = timeoutIni; // time of error start
                        dif = getDiference(to, tPrevious);// time difference
                        samplesDT.addValue(dif);
                    }
                    untrusted = true;
                } else { // trusted
                    timeoutIni = to[id];
                    if ((lin != 1) && untrusted) {// if before the system is untrusted
                        mistakeTime += ts - errorIni; // increases the error time
                        error++; // count errors
                    }
                    untrusted = false;
                }
                timeoutPrev = timeoutIni;
                tEnd = ts;
                if (A[id].size() == sizeList) { // if queue is full
                    A[id].poll();// remove(sizeList-1);
                }
                A[id].add(new DeviceReply(mid, ts));
                tPrevious[id] = ts;
                lin++;
                EA = (long) computeEAWithMsgId(mid, id); // calculate the estimated arrival of the next heartbeat
                to[id] = EA + margin; // estimated arrival + safety margin
                timeoutIni = to[id];
            } // end of file
            totTime = ts - tsIni; // total time
            rate = (double) error / ((double) totTime / 1000000000); // error rate
            pa = (1 - ((double) mistakeTime / (double) totTime)); // calculate pa

            os = new FileOutputStream("statistic.txt", true);
            osw = new OutputStreamWriter(os);
            bw = new BufferedWriter(osw);

            bw.write("#server;nset;sizelist;margin;threshold;error;mistake time;error rate;total_time;pa;td_mean;td_max\n");
            bw.write(server + ";" + nset + ";" + sizeList + ";" + margin + ";"
                    + this.threshold + ";" + error + ";" + mistakeTime
                    + ";" + f.format(rate) + ";" + totTime + ";"
                    + f.format(pa) + ";" + f.format(samplesDT.getMean() / 1000000) + ";"
                    + f.format(samplesDT.getMax() / 1000000) + "\n");

            sc.close();
            bw.close();
            inputStream.close();
        } catch (IOException | NumberFormatException ex) {
            Logger.getLogger(FDImpact.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public int sumTrusted() { /// sum the impact factor of trusted processes
        int trusLevel = 0;
        for (int i = 0; i < 10; i++) {
            if (trusted[i]) {
                trusLevel += this.set[i];
            }
        }
        return trusLevel;
    }

    // calculate the estimated arrival of the next heartbeat
    public double computeEAWithMsgId(long l, int id) {
        // id of node
        // l = highest number of heartbeat sequence received
        double partial = 0, tot = 0, avg = 0;
        DeviceReply dr;
        try {
            Queue<DeviceReply> q = new LinkedList<>();
            q.addAll(this.A[id]);
            int n = q.size();
            while (!q.isEmpty()) {
                dr = q.poll();
                partial = dr.arrivalTime - (this.Ai * dr.mid);
                tot += partial;
            }
            if (l > 0) {
                avg = tot / n + (l + 1) * this.Ai;
            }
            return avg;
        } catch (Exception e) {
            System.out.println("ERRO " + e.getMessage());
            return 0;
        }
    }

    // calculate the estimated arrival of the next heartbeat
    public double computeEA(long l, int id) {
        // id of node
        // l = highest number of heartbeat sequence received
        double tot = 0, avg = 0;
        int i = 0;
        long ts;
        try {
            Queue<DeviceReply> q = new LinkedList<>();
            q.addAll(A[id]);
            while (!q.isEmpty()) {
                ts = q.poll().arrivalTime;
                i++;
                tot += ts - (Ai * i);
            }
            if (l > 0) {
                avg = ((1 / (double) l) * ((double) tot)) + (((double) l + 1) * Ai);
            }
            System.out.println("l = " + l + ", id = " + id + ", avg = " + avg);
            return avg;
        } catch (Exception e) {
            System.out.println("ERRO " + e.getMessage());
            return 0;
        }
    }

    // calculate time difference between the current timestamp and the previous
    // takes the largest difference between all nodes
    public long getDiference(long[] to, long[] tPrev) {
        long dif = 0, timeDif;
        for (int i = 0; i < 10; i++) {
            timeDif = (to[i] - tPrev[i]);
            if (timeDif > dif) {
                dif = timeDif;
            }
        }
        return dif;
    }
}
