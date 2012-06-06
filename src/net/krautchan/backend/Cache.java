package net.krautchan.backend;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import net.krautchan.data.*;
/*
* Copyright (C) 2012 Johannes Jander (johannes@jandermail.de)
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
* http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/

public class Cache<T extends KrautObject> {
	private ConcurrentLinkedQueue<T> krautObjects = new ConcurrentLinkedQueue<T>();
	private int capacity = 200;

	public Cache() {
		super();
	}
	
	public Cache(int capacity) {
		this.capacity = capacity;
	}

	public void add (T obj) {
		obj.cachedTime = new Date().getTime();
		krautObjects.add(obj);
		if (krautObjects.size() > capacity) {
			trimCache (krautObjects.size() - capacity);
		}
	}
	
	public void add (Collection<T> objs) {
		for (T obj : objs) {
			add (obj);
		}
	}
	
	public T get (Long id) {
		Iterator<T> iter = krautObjects.iterator();
		while (iter.hasNext()) {
			T obj = iter.next();
			if (obj.dbId.equals(id)) {
				return obj;
			}
		}
		return null;
	}
	
	
	private void trimCache (int numEntriesToRemove) {
		List<T> sorter = new ArrayList<T>();
		Iterator<T> iter = krautObjects.iterator();
		int count = 0;
		while (iter.hasNext()) {
			T obj = iter.next();
			sorter.add(obj);
			count++;
		}
		Collections.sort(sorter, new Comparator <T>() {
			@Override
			public int compare(T arg0, T arg1) {
				return (int) (arg0.cachedTime - arg1.cachedTime);
			}
		});
		for (int i = sorter.size()-1; i > numEntriesToRemove; i--) {
			krautObjects.remove(sorter.get(i));
		}
	}

}
