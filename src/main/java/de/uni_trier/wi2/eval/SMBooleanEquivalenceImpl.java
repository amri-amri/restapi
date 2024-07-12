package de.uni_trier.wi2.eval;

import de.uni_trier.wi2.naming.*;
import de.uni_trier.wi2.procake.data.model.*;
import de.uni_trier.wi2.procake.data.object.*;
import de.uni_trier.wi2.procake.data.object.base.*;
import de.uni_trier.wi2.procake.similarity.*;
import de.uni_trier.wi2.procake.similarity.impl.*;

public class SMBooleanEquivalenceImpl extends SimilarityMeasureImpl implements SMBooleanEquivalence{
    @Override
    public boolean isSimilarityFor(DataClass dataClass, String s) {
        return dataClass.isBoolean() || Utils.isClassOrSublassOf(dataClass, Classnames.getXESClassName(Classnames.BOOLEAN_CLASS));
    }

    @Override
    public Similarity compute(DataObject dataObject, DataObject dataObject1, SimilarityValuator similarityValuator) {
        if (!isSimilarityFor(dataObject.getDataClass(),null) || !isSimilarityFor(dataObject1.getDataClass(),null)) return new SimilarityImpl(this, dataObject, dataObject1);

        boolean b,b1;
        if (dataObject.isBoolean()) b = ((BooleanObject) dataObject).getNativeBoolean();
        else b = ((BooleanObject) ((AggregateObject) dataObject).getAttributeValue(XESorAggregateAttributeNames.VALUE)).getNativeBoolean();

        if (dataObject1.isBoolean()) b1 = ((BooleanObject) dataObject1).getNativeBoolean();
        else b1 = ((BooleanObject) ((AggregateObject) dataObject1).getAttributeValue(XESorAggregateAttributeNames.VALUE)).getNativeBoolean();

        return new SimilarityImpl(this, dataObject, dataObject1, (b==b1)?1:0);
    }

    @Override
    public String getSystemName() {
        return SMBooleanEquivalence.NAME;
    }
}
