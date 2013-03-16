package net.krautchan.backend;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

import net.krautchan.data.KODataListener;
import net.krautchan.data.KrautObject;

public class KCCache<T extends KrautObject> implements KODataListener<T> {
	private CachePersister<T> persister;
	private ConcurrentLinkedQueue<T> krautObjects = new ConcurrentLinkedQueue<T>();
	private int capacity = 200;

	public KCCache() {
		super();
	}
	
	public KCCache(int capacity) {
		this.capacity = capacity;
	}
	
	public void setPersister(CachePersister<T> persister) {
		this.persister = persister;
	}

	public void add (T obj) {
		obj.cachedTime = new Date().getTime();
		if (krautObjects.contains(obj)) {
			krautObjects.remove(obj);
		}
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
	
	public List<T>getAll () {
		List<T> retVal = new ArrayList<T>();
		retVal.addAll(krautObjects);
		return retVal;
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
		List<T> removed = new ArrayList<T>();
		for (int i = sorter.size()-1; i > numEntriesToRemove; i--) {
			krautObjects.remove(sorter.get(i));
			removed.add(sorter.get(i));
		}
		if (null != persister) {
			persister.persist(removed);
		}
	}
	
	public void freeze() {
		if (null != persister) {
			persister.persist(krautObjects);
		}
	}
	
	public void thaw() {
		if (null != persister) {
			krautObjects.clear();
			krautObjects.addAll(persister.retrieveAll());
		}
	}
	
	public int size() {
		return krautObjects.size();
	}
	
	public void clear() {
		krautObjects.clear();
	}

	@Override
	public void notifyAdded(T item, Object token) {
		add(item);
	}

	@Override
	public void notifyDone(Object token) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void notifyError(Exception ex, Object token) {
		// TODO Auto-generated method stub
	}
	
	public interface CachePersister<T extends KrautObject> {
		public void persist(T obj);
		public void persist(Collection<T> objects);
		public T retrieve(Long dbId);
		public Collection<T> retrieveAll();
		
	}

}
