package edu.berkeley.nlp.util;

import java.util.Collection;

public class FMeasureStats {

    private double alpha = 1.0;

    private int tpTotal = 0;
    private int fpTotal = 0;
    private int fnTotal = 0;

    private Stats precStats ;
    private Stats recStats ;
    private Stats fmeasStats ;

    public FMeasureStats(double alpha) {        
        precStats = new Stats();
        recStats = new Stats();
        fmeasStats = new Stats();
        this.alpha = alpha;
    }
    
    public FMeasureStats() {        
        this(1.0);
    }


    public void observe(Collection<?> goldObs, Collection<?> proposObs) {
        int tp = 0;
        int fp = 0;
        int fn = 0;
        for (Object o: proposObs) {
            if (proposObs.contains(o))  {
                tp ++;
            } else {
                fp ++;
            }
        }
        for (Object o: goldObs) {
            if (!proposObs.contains(o)) {
                fn++;
            }
        }

        observe(tp, fp, fn);
    }

    public void observe(int tp, int fp, int fn) {
        tpTotal += tp;
        fpTotal += fp;
        fnTotal += fn;
        double prec = computeProb(tp, fp);
        double recall = computeProb(tp, fn);
        double fmeas = computeFMeasure(prec, recall);
        precStats.observe(prec);
        recStats.observe(recall);
        fmeasStats.observe(fmeas);
    }


    private double computeProb(int tp, int fp) {
        double prec   = tp > 0  ? tp / (((double) tp) + ((double) fp)) : 0.0;
        return prec;
    }


    private double computeFMeasure(double prec, double recall) {
        double fmeas = prec > 0.0 && recall > 0.0 ? ((alpha+1.0) * prec * recall) / (alpha*(prec+recall)) : 0.0;
        return fmeas;
    }
    
    public double getAveragePrecision() {
        return precStats.getAverage();
    }
    
    public double getAverageRecall() {
        return recStats.getAverage();
    }
    
    public double getMicroAverageFMeasure() {
        return fmeasStats.getAverage();
    }
    
    public double getMacroAveragePrecision() {
        double p = computeProb(tpTotal, fpTotal);
        return p;
    }
    public double getMacroAverageRecall() {
        double r = computeProb(tpTotal, fnTotal);
        return r;
    }
    
    public double getMacroAverageFMeasure() {
        double p = getMacroAveragePrecision();
        double r = getMacroAverageRecall();
        return computeFMeasure(p, r);
    }
    
    public String toString() {
        return String.format("precision: %.5f\nrecall: %.5f\nfmeas: %.5f\n", 
        getMacroAveragePrecision(),getMacroAverageRecall(),getMicroAverageFMeasure());        
    }
}
