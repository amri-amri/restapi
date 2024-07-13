package de.uni_trier.wi2.eval;

import de.uni_trier.wi2.procake.data.model.*;
import de.uni_trier.wi2.procake.data.object.*;
import de.uni_trier.wi2.procake.data.object.base.*;
import de.uni_trier.wi2.procake.similarity.*;
import de.uni_trier.wi2.procake.similarity.impl.*;

import java.sql.*;

import static de.uni_trier.wi2.eval.Utils.*;

public class SMNumericComparison100Impl extends SimilarityMeasureImpl implements SMNumericComparison100 {
    @Override
    public boolean isSimilarityFor(DataClass dataClass, String s) {
        Model model = dataClass.getModel();
        for (String className : SMNumericComparison100.applicableClasses) {
            if (isClassOrSublassOf(dataClass, className)) return true;
        }
        return false;
    }

    @Override
    public Similarity compute(DataObject dataObject, DataObject dataObject1, SimilarityValuator similarityValuator) {

        Model model = dataObject.getModel();
        DataClass dataClass, dataClass1;
        dataClass = dataObject.getDataClass();
        dataClass1 = dataObject1.getDataClass();

        if (!isSimilarityFor(dataClass, null) || !isSimilarityFor(dataClass1, null))
            return new SimilarityImpl(this, dataObject, dataObject1, 0);


        String qKey = ((StringObject) ((AggregateObject) dataObject).getAttributeValue("key")).getNativeString();
        String cKey = ((StringObject) ((AggregateObject) dataObject).getAttributeValue("key")).getNativeString();

        if (!qKey.equals(cKey)) return new SimilarityImpl(this, dataObject, dataObject1, 0);

        DataObject qVal = ((AggregateObject) dataObject).getAttributeValue("value");
        DataObject cVal = ((AggregateObject) dataObject).getAttributeValue("value");

        double qNum, cNum;


        if (areBothClassOrSubclassOf(dataClass, dataClass1, "XESDateTimeClass")) {
            Timestamp qTst = ((TimestampObject) qVal).getNativeTimestamp();
            Timestamp cTst = ((TimestampObject) cVal).getNativeTimestamp();

            qNum = qTst.getTime();
            cNum = cTst.getTime();
        } else if (areBothClassOrSubclassOf(dataClass, dataClass1, "XESIntegerClass")) {
            qNum = ((IntegerObject) qVal).getNativeInteger();
            cNum = ((IntegerObject) cVal).getNativeInteger();

        } else if (areBothClassOrSubclassOf(dataClass, dataClass1, "XESFloatClass")) {
            qNum = ((DoubleObject) qVal).getNativeDouble();
            cNum = ((DoubleObject) cVal).getNativeDouble();

        } else if (areBothClassOrSubclassOf(dataClass, dataClass1, "DateClass")) {
            qNum = ((DateObject) qVal).getNativeDate().getTime();
            cNum = ((DateObject) cVal).getNativeDate().getTime();

        } else if (areBothClassOrSubclassOf(dataClass, dataClass1, "TimeClass")) {
            qNum = ((TimeObject) qVal).getNativeTime().getTime();
            cNum = ((TimeObject) cVal).getNativeTime().getTime();

        } else if (areBothClassOrSubclassOf(dataClass, dataClass1, "DateTimeClass")) {
            qNum = ((TimestampObject) qVal).getNativeTimestamp().getTime();
            cNum = ((TimestampObject) cVal).getNativeTimestamp().getTime();

        } else if (areBothClassOrSubclassOf(dataClass, dataClass1, "IntegerClass")) {
            qNum = ((IntegerObject) qVal).getNativeDouble();
            cNum = ((IntegerObject) cVal).getNativeDouble();

        } else if (areBothClassOrSubclassOf(dataClass, dataClass1, "DoubleClass")) {
            qNum = ((DoubleObject) qVal).getNativeDouble();
            cNum = ((DoubleObject) cVal).getNativeDouble();

        } else {
            return new SimilarityImpl(this, dataObject, dataObject1, 0);
        }

        double sim = 100 - Math.min(100, Math.abs(qNum - cNum));
        sim /= 100;
        return new SimilarityImpl(this, dataObject, dataObject1, sim);


    }

    @Override
    public String getSystemName() {
        return SMNumericComparison100.NAME;
    }


}

