package orbit.launcher;

import java.util.Collection;
import java.util.Properties;


/**
 * This interface describes the class used to configure the launcher.  It
 * contains 3 kinds of property mappings - defaults, overrides, and regular
 * mappings.  If an override is set for a given key, its value is always
 * returned.  If a regular mapping has been set by the user, its value is
 * returned only if the same key has not been set as an override.  If a
 * default mapping is set, it is returned only if there is no override or
 * regular mapping.  If no mapping exists for a given key, an
 * IllegalArgumentException will be thrown
 */
public interface Preferences
{

    public static final String LIST_SEPARATOR_TOKEN = "!";

    /**
     * Returns true if the property is found and contains the string true.
     * Returns false if the property is found and contains any value other
     * than the string true.
     * 
     * @param key The name of the property to look up
     * @return boolean
     */
    public boolean getBool(String key);

    /**
     * Returns the named property or the default value if the property is not
     * found.  Returns true if the property is found AND it is the string
     * 'true'. Returns false if the property is found and is any string other
     * than 'true'. Returns defaultValue if the property is not found.
     * 
     * @param key The name of the property to look up
     * @param defaultValue The value to return if the property is not found
     * @return boolean
     */
    public boolean getBool(String key, boolean defaultValue);

    /**
     * Sets the property named by key to be value
     * 
     * @param key The name of the property to look up
     * @param value The value to set the property to
     */
    public void setBool(String key, boolean value);

    /**
     * Method getChar.
     * 
     * @param key The name of the property to look up
     * @return char
     */
    public char getChar(String key);

    /**
     * Returns a single character property, or the default value if the
     * property is not found.
     * 
     * @param key The name of the property to look up
     * @param defaultValue The value to return if the property is not found
     * @return char
     */
    public char getChar(String key, char defaultValue);

    /**
     * Sets the property named by key to be value
     * 
     * @param key The name of the property to look up DOCUMENT ME!
     * @param value The value to set the property to DOCUMENT ME!
     */
    public void setChar(String key, char value);

    /**
     * Method getDouble.
     * 
     * @param key The name of the property to look up
     * @return double
     */
    public double getDouble(String key);

    /**
     * If the property is found, it is parsed as a double, using
     * Double.parseDouble(), throwing a NumberFormatException if the property
     * is not a valid double. Returns defaultValue if the property is not
     * found.
     * 
     * @param key The name of the property to look up
     * @param defaultValue The value to return if the property is not found
     * @return double
     */
    public double getDouble(String key, double defaultValue);

    /**
     * Sets the property named by key to be value
     * 
     * @param key The name of the property to look up DOCUMENT ME!
     * @param value The value to set the property to DOCUMENT ME!
     */
    public void setDouble(String key, double value);

    /**
     * Method getInt.
     * 
     * @param key The name of the property to look up
     * @return int
     */
    public int getInt(String key);

    /**
     * If the property is found, it is parsed as an int, using
     * Integer.parseInt(), throwing a NumberFormatException if the property
     * is not a valid int. Returns defaultValue if the property is not found.
     * 
     * @param key The name of the property to look up
     * @param defaultValue The value to return if the property is not found
     * @return int
     */
    public int getInt(String key, int defaultValue);

    /**
     * Sets the property named by key to be value
     * 
     * @param key The name of the property to look up DOCUMENT ME!
     * @param value The value to set the property to DOCUMENT ME!
     */
    public void setInt(String key, int value);

    /**
     * Method getLong.
     * 
     * @param key The name of the property to look up
     * @return long
     */
    public long getLong(String key);

    /**
     * If the property is found, it is parsed as an long, using
     * Long.parseLong(), throwing a NumberFormatException if the property is
     * not a valid Long. Returns defaultValue if the property is not found.
     * 
     * @param key The name of the property to look up
     * @param defaultValue The value to return if the property is not found
     * @return long
     */
    public long getLong(String key, long defaultValue);

    /**
     * Sets the property named by key to be value
     * 
     * @param key The name of the property to look up DOCUMENT ME!
     * @param value The value to set the property to DOCUMENT ME!
     */
    public void setLong(String key, long value);

    /**
     * Method getString.
     * 
     * @param key The name of the property to look up
     * @return String
     */
    public String getString(String key);

    /**
     * Returns the named property. If the property is not found the default
     * value is returned.
     * 
     * @param key The name of the property to look up
     * @param defaultValue The value to return if the property is not found
     * @return String
     */
    public String getString(String key, String defaultValue);

    /**
     * Sets the property named by key to be value
     * 
     * @param key The name of the property to look up DOCUMENT ME!
     * @param value The value to set the property to DOCUMENT ME!
     */
    public void setString(String key, String value);

    /**
     * Returns a collection created by tokenizing the named property.
     * 
     * @param key The name of the property to look up
     * @return Collection
     */
    public Collection getStringList(String key);

    /**
     * Returns a collection created by tokenizing the named property. If the
     * property is not found the default value is returned.
     * 
     * @param key The name of the property to look up
     * @param defaultValue The value to return if the property is not found
     * @return Collection
     */
    public Collection getStringList(String key, Collection defaultValue);

    /**
     * Sets the property named by key to be value
     * 
     * @param key The name of the property to look up DOCUMENT ME!
     * @param value The value to set the property to DOCUMENT ME!
     */
    public void setStringList(String key, Collection value);

    /**
     * Returns a Properties object that represents this Preferences
     * 
     * @return Properties
     */
    public Properties toProperties();

    /**
     * Allows one to set an override property directly
     * 
     * @param key The name of the property to look up The property's key
     * @param value The value to set the property to The property's value
     */
    public void setOverride(String key, String value);

    /**
     * Checks if key is an overriden property
     * 
     * @param key The name of the property to look up
     * @return boolean
     */
    public boolean isOverride(String key);

    /**
     * Checks if key is an defaulted property
     * 
     * @param key The name of the property to look up
     * @return boolean
     */
    public boolean isDefault(String key);

    /**
     * This method replaces each properties value with variable references, ,
     * expanded by a lookup in self.     If the variable does not exist in
     * self, it is left unexpanded     in the result string.
     */
    public void expandVariables();
}
