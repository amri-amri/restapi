package de.uni_trier.wi2.eval;

import de.uni_trier.wi2.procake.similarity.*;

public interface SMNumericComparison100 extends SimilarityMeasure {
    String NAME = "NumericComparison100";

    String[] applicableClasses = new String[]{
            //"Date",
            //"Time",
            //"Timestamp",
            //"XESDateTimeClass",
            "XESIntegerClass",
            "XESFloatClass",
    };

}
