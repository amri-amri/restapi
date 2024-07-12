package de.uni_trier.wi2.eval;

import de.uni_trier.wi2.procake.similarity.*;

import java.util.*;

public interface SMChronologicalOrNumericComparison100 extends SimilarityMeasure {
    String NAME = "ChronologicalOrNumericComparison100";

    String[] applicableClasses = new String[]{
            //"Date",
            //"Time",
            //"Timestamp",
            "XESDateTimeClass",
            "XESIntegerClass",
            "XESFloatClass",
    };

}
