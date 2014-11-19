package com.lizard.fastdb.persistence;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Key大小写不区分HashMap
 *
 * @author SHEN.GANG
 */
public class CaseInsensitiveHashMap<V> extends LinkedHashMap<String, V>
{
	private static final long	serialVersionUID	= -1411919070813437264L;
	
	/**
	 * The internal mapping from lowercase keys to the real keys.
	 * <p>
	 * Any query operation using the key ({@link #get(Object)},
	 * {@link #containsKey(Object)}) is done in three steps:
	 * <ul>
	 * <li>convert the parameter key to lower case</li>
	 * <li>get the actual key that corresponds to the lower case key</li>
	 * <li>query the map with the actual key</li>
	 * </ul>
	 * </p>
	 */
	private final Map<String, String>	lowerCaseMap		= new LinkedHashMap<String, String>();

	@Override
	public boolean containsKey(Object key)
	{
		Object realKey = lowerCaseMap.get(key.toString().toLowerCase());
		return super.containsKey(realKey);
		// Possible optimisation here:
		// Since the lowerCaseMap contains a mapping for all the keys,
		// we could just do this:
		// return lowerCaseMap.containsKey(key.toString().toLowerCase());
	}

	@Override
	public V get(Object key)
	{
		Object realKey = lowerCaseMap.get(key.toString().toLowerCase());
		return super.get(realKey);
	}

	@Override
	public V put(String key, V value)
	{
		/*
		 * In order to keep the map and lowerCaseMap synchronized, we have to
		 * remove the old mapping before putting the new one. Indeed, oldKey and
		 * key are not necessaliry equals. (That's why we call
		 * super.remove(oldKey) and not just super.put(key, value))
		 */
		Object oldKey = lowerCaseMap.put(key.toLowerCase(), key);
		V oldValue = super.remove(oldKey);
		super.put(key, value);
		return oldValue;
	}

	@Override
	public void putAll(Map<? extends String, ? extends V> m)
	{
		for ( Map.Entry<? extends String, ? extends V> entry : m.entrySet() )
		{
			this.put(entry.getKey(), entry.getValue());
		}
	}

	@Override
	public V remove(Object key)
	{
		Object realKey = lowerCaseMap.remove(key.toString().toLowerCase());
		return super.remove(realKey);
	}
}
