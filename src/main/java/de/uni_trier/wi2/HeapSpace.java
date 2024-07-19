package de.uni_trier.wi2;

import org.slf4j.*;

public class HeapSpace {

    static float maxHeapSpace = 0;

    public static void measure(){
        float heapSpace = (float) (Math.round(Runtime.getRuntime().totalMemory()/1E7)/1E2);
        if (heapSpace > maxHeapSpace) maxHeapSpace = heapSpace;
    }

    public static float getGBandWipe(){
        float a =  maxHeapSpace;
        maxHeapSpace = 0;
        return a;
    }

    //public static void log(Class c, String msg){
    //    Logger logger = LoggerFactory.getLogger(c);
    //    logger.info(String.format("%smax heap space measured: %.2f GB",msg==null?"":msg+" ", maxHeapSpace));
    //}


}
