package net.krautchan.data;

/*
* Copyright (C) 2011 Johannes Jander (johannes@jandermail.de)
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

import java.io.Serializable;


public abstract class KrautObject implements Serializable {
	protected static final String baseUrl = "http://krautchan.net/";
	/** Krautchan's posting-IDs are not unique across boards, boards have no ID
	 * so we build our own unique id for every data type we transport.
	 * 
	 * NOTE: the contract makes NO stipulations about those IDs being sequential
	 * neither do they have anything to do with the KC-IDs
	 **/
	public Long dbId;
	public String uri;
	public DataEventType type;
	public transient long cachedTime=0; 
	
	public enum DataEventType {
	    ADD,
	    REMOVE,
	    MODIFY
	}
	
}
