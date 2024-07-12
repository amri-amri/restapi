package de.uni_trier.wi2.eval;

import de.uni_trier.wi2.procake.data.model.*;

public class Utils {
    public static boolean isClassOrSublassOf(DataClass dataClass, String className) {
        DataClass dataClass1 = dataClass.getModel().getClass(className);
        return isClassOrSublassOf(dataClass, dataClass1);
    }

    public static boolean isClassOrSublassOf(DataClass dataClass, DataClass dataClass1) {
        if (dataClass.getName().equals(dataClass1.getName())) return true;
        return dataClass.isSubclassOf(dataClass1);
    }

    public static boolean areBothClassOrSubclassOf(DataClass dataClass, DataClass dataClass1, String className) {
        DataClass dataClass2 = dataClass.getModel().getClass(className);
        return areBothClassOrSubclassOf(dataClass, dataClass1, dataClass2);
    }

    public static boolean areBothClassOrSubclassOf(DataClass dataClass, DataClass dataClass1, DataClass dataClass2) {
        return isClassOrSublassOf(dataClass, dataClass2) && isClassOrSublassOf(dataClass1, dataClass2);
    }
}
