/*
 * Copyright 2012-2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package be.ordina.msdashboard.stores;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import be.ordina.msdashboard.cache.NodeCache;
import be.ordina.msdashboard.converters.JsonToObjectConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;

import be.ordina.msdashboard.constants.Constants;
import be.ordina.msdashboard.model.Node;
import rx.Observable;

public class RedisStore implements NodeCache, NodeStore {

	private RedisTemplate<String, Object> redisTemplate;

	private RedisConnectionFactory redisConnectionFactory;

	@Autowired
	public RedisStore(final RedisTemplate<String, Object> redisTemplate,
					  final RedisConnectionFactory redisConnectionFactory) {
		this.redisTemplate = redisTemplate;
		((JedisConnectionFactory) redisConnectionFactory).setTimeout(10000);
		this.redisConnectionFactory = redisConnectionFactory;
	}

	@Override
	public Collection<Node> getAllNodes() {
		List<Node> results = new ArrayList<>();
		Set<String> keys = redisTemplate.keys(KEY_PREFIX + "*");
		for (String key : keys) {
			Node node = (Node) redisTemplate.opsForValue().get(key);
			results.add(node);
		}
		return results;
	}

	@Override
	public Observable<Node> getAllNodesAsObservable() {
		return Observable.from(getAllNodes());
	}

	@Override
	public void saveNode(final String nodeData) {
		Node node = getNode(nodeData);
		String nodeId = node.getId();
		node.getDetails().put(VIRTUAL_FLAG, true);
		redisTemplate.opsForValue().set(KEY_PREFIX + nodeId, node);
		evictGraphCache();
	}

	@Override
	public void deleteNode(final String nodeId) {
		redisTemplate.delete(nodeId);
		evictGraphCache();
	}

	@Override
	public void deleteAllNodes() {
		redisTemplate.delete(redisTemplate.keys("*"));
		evictGraphCache();
	}

	private Node getNode(String nodeData) {
		JsonToObjectConverter<Node> converter = new JsonToObjectConverter<>(Node.class);
		return converter.convert(nodeData);
	}

	@Override
	public void flushDB() {
		redisConnectionFactory.getConnection().flushDb();
	}

	@Override
	@CacheEvict(value = Constants.GRAPH_CACHE_NAME, allEntries = true)
	public void evictGraphCache() {
	}

	@Override
	@CacheEvict(value = Constants.HEALTH_CACHE_NAME, allEntries = true)
	public void evictHealthsCache() {
	}

	@Override
	@CacheEvict(value = Constants.INDEX_CACHE_NAME, allEntries = true)
	public void evictIndexesCache() {
	}

	@Override
	@CacheEvict(value = Constants.PACTS_CACHE_NAME, allEntries = true)
	public void evictPactsCache() {
	}
}