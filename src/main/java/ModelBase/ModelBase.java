package ModelBase;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ModelBase {

    private Map<String, Object> dictionary;
    Map<String, List<String>> mappeable;

    public ModelBase(Map<String, List<String>> mappeable) {
        this.mappeable = mappeable;
        this.dictionary = new HashMap<>();
    }
    
    // Add a key-value pair to the dictionary
    public void add(String key, Object value) {
        dictionary.put(key, value);
    }

    // Get the value associated with a key in the dictionary
    public Object get(String key) {
        return dictionary.get(key);
    }

    // Check if a key exists in the dictionary
    public boolean containsKey(String key) {
        return dictionary.containsKey(key);
    }

    // Remove a key and its associated value from the dictionary
    public void remove(String key) {
        dictionary.remove(key);
    }

    // Print the content of the dictionary recursively
    public void print() {
        printRecursive(dictionary, 0);
    }

    // Recursive function to print nested dictionaries
    private void printRecursive(Map<String, Object> dict, int indentLevel) {
        String indentation = " ".repeat(indentLevel * 2);
        for (Map.Entry<String, Object> entry : dict.entrySet()) {
            System.out.println(indentation + entry.getKey() + ": " + entry.getValue());
            if (entry.getValue() instanceof Map) {
                printRecursive((Map<String, Object>) entry.getValue(), indentLevel + 1);
            }
        }
    }

//    public static void main(String[] args) {
//        ModelBase ModelBase = new ModelBase();
//
//        // Add elements to the dictionary
//        ModelBase.add("key1", "value1");
//        ModelBase.add("key2", "value2");
//
//        // Add a nested dictionary
//        Map<String, Object> nestedDict = new HashMap<>();
//        nestedDict.put("nestedKey1", "nestedValue1");
//        nestedDict.put("nestedKey2", "nestedValue2");
//        ModelBase.add("ModelBase", nestedDict);
//
//        // Add a nested dictionary
//        Map<String, Object> nestedDict2 = new HashMap<>();
//        nestedDict2.put("nestedKey3", "nestedValue3");
//        nestedDict2.put("nestedKey4", "nestedValue4");
//        ModelBase.add("ModelBase2", nestedDict2);
//
//        // Add a nested dictionary
//        Map<String, Object> nestedDict3 = new HashMap<>();
//        nestedDict2.put("nestedKey5", "nestedValue5");
//        nestedDict2.put("nestedKey6", "nestedValue6");
//        ModelBase.add("ModelBase2", nestedDict2);
//
//        // Print the content of the dictionary
//        ModelBase.print();
//
//    }
}

