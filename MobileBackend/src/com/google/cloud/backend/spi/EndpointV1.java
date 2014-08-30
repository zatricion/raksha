/*
 * Copyright (c) 2013 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.google.cloud.backend.spi;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.github.davidmoten.geo.Coverage;
import com.github.davidmoten.geo.GeoHash;
import com.github.davidmoten.geo.LatLong;
import com.google.android.gcm.server.Message;
import com.google.android.gcm.server.Result;
import com.google.android.gcm.server.Sender;
import com.google.api.server.spi.config.AnnotationBoolean;
import com.google.api.server.spi.config.Api;
import com.google.api.server.spi.config.ApiMethod;
import com.google.api.server.spi.config.ApiMethod.HttpMethod;
import com.google.api.server.spi.config.ApiNamespace;
import com.google.api.server.spi.response.BadRequestException;
import com.google.api.server.spi.response.NotFoundException;
import com.google.api.server.spi.response.UnauthorizedException;
import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.PreparedQuery;
import com.google.appengine.api.datastore.Query;
import com.google.appengine.api.datastore.Query.Filter;
import com.google.appengine.api.datastore.Query.FilterPredicate;
import com.google.appengine.api.datastore.Query.FilterOperator;
import com.google.appengine.api.datastore.Query.CompositeFilterOperator;
import com.google.appengine.api.datastore.Query.SortDirection;
import com.google.appengine.api.users.User;
import com.google.cloud.backend.beans.EntityDto;
import com.google.cloud.backend.beans.EntityListDto;
import com.google.cloud.backend.beans.QueryDto;
import com.google.cloud.backend.config.BackendConfigManager;
import com.github.davidmoten.geo.GeoHash;

import javax.inject.Named;

import java.util.logging.Logger;

/**
 * An endpoint for all CloudBackend requests.
 */
@Api(name = "mobilebackend", namespace = @ApiNamespace(ownerDomain = "google.com",
    ownerName = "google.com", packagePath = "cloud.backend.android"),
    useDatastoreForAdditionalConfig = AnnotationBoolean.TRUE)
public class EndpointV1 {
	private static int M2LAT = 111111;
	private static final Logger Log = Logger.getLogger(EndpointV1.class.getName());

  /**
   * Inserts a CloudEntity on the backend. If it does not have any Id, it
   * creates a new Entity. If it has, find the existing entity and update it.
   *
   * @param cd
   *          {@link EntityDto} for inserting a CloudEntity.
   * @param user
   *          {@link User} who called this request.
   * @return {@link EntityDto} that has updated fields (like updatedAt and new
   *         Id).
   * @throws UnauthorizedException
   *           if the requesting {@link User} has no sufficient permission for
   *           the operation.
   */
  @ApiMethod(path = "CloudEntities/insert/{kind}", httpMethod = HttpMethod.POST)
  public EntityDto insert(@Named("kind") String kindName, EntityDto cd, User user)
      throws UnauthorizedException {

    SecurityChecker.getInstance().checkIfUserIsAvailable(user);
    EntityListDto cdl = new EntityListDto();
    cdl.add(cd);
    CrudOperations.getInstance().saveAll(cdl, user);
    return cd;
  }
  
  private void sendAlerts(String geohash, String from_name, double radius) throws IOException {
    int GCM_SEND_RETRIES = 3;
    // Get the Datastore Service
    DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
    		
	BackendConfigManager backendConfigManager = new BackendConfigManager();

	String gcmKey = backendConfigManager.getGcmKey();
    boolean isGcmKeySet = !(gcmKey == null || gcmKey.trim().length() == 0);

    // get bounding box hashes from location and radius
    LatLong pos = GeoHash.decodeHash(geohash);
    Log.info(geohash);
    Log.info(pos.toString());
    double lat = pos.getLat();
    double lon = pos.getLon();
    Coverage cover = GeoHash.coverBoundingBox((lat + radius / M2LAT), // top left lat
											  (lon - radius / (M2LAT * Math.cos(Math.toRadians(lat)))), // top left lon
											  (lat - radius / M2LAT), // bottom right lat
											  (lon + radius / (M2LAT * Math.cos(Math.toRadians(lat))))); // bottom right lon

    Log.info(cover.toString());
    Set<String> ghSet = cover.getHashes();
    List<String> regIdList = new ArrayList<String>();
    // Get regIds from prefix search of bounding box hashes
    for (String gh : ghSet) {
    	Filter ghMinFilter =
		  new FilterPredicate("location",
		                      FilterOperator.GREATER_THAN_OR_EQUAL,
		                      gh);
	
		Filter ghMaxFilter =
		  new FilterPredicate("location",
		                      FilterOperator.LESS_THAN_OR_EQUAL,
		                      (gh + Character.MAX_VALUE));
	
		//Use CompositeFilter to combine multiple filters
		Filter ghRangeFilter =
		  CompositeFilterOperator.and(ghMinFilter, ghMaxFilter);
	
	
		// Use class Query to assemble a query
		Query q = new Query("Person")
			.setFilter(ghRangeFilter)
			.addSort("location", SortDirection.DESCENDING);
	
		// Use PreparedQuery interface to retrieve results
		PreparedQuery pq = datastore.prepare(q);
	
		for (Entity result : pq.asIterable()) {
		  String regId = (String) result.getProperty("regId");
		  regIdList.add(regId);
		}
    }
    
    // Only attempt to send GCM if GcmKey is available
    if (isGcmKeySet && !regIdList.isEmpty()) {
      Sender sender = new Sender(gcmKey);
      Message message = new Message.Builder().addData("from_name", from_name).build();
      sender.send(message, regIdList, GCM_SEND_RETRIES);
    }
  }
  
  /**
   * Updates a CloudEntity on the backend. If it does not have any Id, it
   * creates a new Entity. If it has, find the existing entity and update it.
   *
   * @param cd
   *          {@link EntityDto} for inserting/updating a CloudEntity.
   * @param user
   *          {@link User} who called this request.
   * @return {@link EntityDto} that has updated fields (like updatedAt and new
   *         Id).
   * @throws UnauthorizedException
   *           if the requesting {@link User} has no sufficient permission for
   *           the operation.
   * @throws IOException 
   */
  @ApiMethod(path = "CloudEntities/update/{kind}", httpMethod = "POST")
  public EntityDto update(@Named("kind") String kindName, EntityDto cd, User user)
      throws UnauthorizedException, IOException {

    SecurityChecker.getInstance().checkIfUserIsAvailable(user);

    // get properties
    @SuppressWarnings("rawtypes")
	Map values = (Map) cd.getProperties();
    for (Object key : values.keySet()) {
      // get property name and value
      String propName = (String) key;
      Object val = values.get(key);
      if ((propName == "alert") && (val == Boolean.TRUE)) {
    	  sendAlerts((String) values.get("location"), (String) values.get("name"), ((Double) values.get("radius")).doubleValue());
    	  break;
      }
    }
    
    EntityListDto cdl = new EntityListDto();
    cdl.add(cd);
    CrudOperations.getInstance().saveAll(cdl, user);
    return cd;
  }

  /**
   * Inserts multiple CloudEntities on the backend. Works just the same as
   * {@link EndpointV1#insert(String, EntityDto, User)}
   *
   * @param cdl
   *          {@link EntityListDto} that holds {@link EntityDto}s to save.
   * @param user
   *          {@link User} who called this request.
   * @return {@link EntityListDto} that has updated {@link EntityDto}s.
   * @throws UnauthorizedException
   *           if the requesting {@link User} has no sufficient permission for
   *           the operation.
   */
  @ApiMethod(path = "CloudEntities/insertAll", httpMethod = HttpMethod.POST)
  // the path need to include the op name to distinguish between saveAll and
  // getAll.
  public EntityListDto insertAll(EntityListDto cdl, User user) throws UnauthorizedException {

    SecurityChecker.getInstance().checkIfUserIsAvailable(user);
    return CrudOperations.getInstance().saveAll(cdl, user);
  }

  /**
   * Updates multiple CloudEntities on the backend. Works just the same as
   * {@link EndpointV1#update(String, EntityDto, User)}
   *
   * @param cdl
   *          {@link EntityListDto} that holds {@link EntityDto}s to save.
   * @param user
   *          {@link User} who called this request.
   * @return {@link EntityListDto} that has updated {@link EntityDto}s.
   * @throws UnauthorizedException
   *           if the requesting {@link User} has no sufficient permission for
   *           the operation.
   */
  @ApiMethod(path = "CloudEntities/updateAll", httpMethod = HttpMethod.POST)
  // the path need to include the op name to distinguish between saveAll and
  // getAll.
  public EntityListDto updateAll(EntityListDto cdl, User user) throws UnauthorizedException {

    SecurityChecker.getInstance().checkIfUserIsAvailable(user);
    return CrudOperations.getInstance().saveAll(cdl, user);
  }

  /**
   * Finds the CloudEntity specified by its Id.
   *
   * @param kindName
   *          Name of the kind for the CloudEntity to get.
   * @param id
   *          Id of the CloudEntity to find.
   * @return {@link EntityDto} of the found CloudEntity.
   * @throws UnauthorizedException
   *           if the requesting {@link User} has no sufficient permission for
   *           the operation.
   * @throws NotFoundException
   *           if the requested CloudEntity has not found
   */
  @ApiMethod(path = "CloudEntities/{kind}/{id}", httpMethod = HttpMethod.GET)
  public EntityDto get(@Named("kind") String kindName, @Named("id") String id, User user)
      throws UnauthorizedException, NotFoundException {

    SecurityChecker.getInstance().checkIfUserIsAvailable(user);
    return CrudOperations.getInstance().getEntity(kindName, id, user);
  }

  /**
   * Finds all the CloudEntities specified by the {@link EntityListDto} of Ids.
   *
   * @param cdl
   *          {@link EntityListDto} that contains a list of Ids to get.
   * @throws UnauthorizedException
   *           if the requesting {@link User} has no sufficient permission for
   *           the operation.
   */
  @ApiMethod(path = "CloudEntities/getAll", httpMethod = HttpMethod.POST)
  public EntityListDto getAll(EntityListDto cdl, User user) throws UnauthorizedException {

    SecurityChecker.getInstance().checkIfUserIsAvailable(user);
    return CrudOperations.getInstance().getAllEntities(cdl, user);
  }

  /**
   * Deletes the CloudEntity specified by its Id.
   *
   * @param kindName
   *          Name of the kind for the CloudEntity to delete.
   * @param id
   *          Id of the CloudEntity to delete.
   * @return {@link EntityDto} a dummy object (Endpoints requires to return any
   *         bean object).
   * @throws UnauthorizedException
   *           if the requesting {@link User} has no sufficient permission for
   *           the operation.
   */
  @ApiMethod(path = "CloudEntities/{kind}/{id}", httpMethod = HttpMethod.DELETE)
  public EntityDto delete(@Named("kind") String kindName, @Named("id") String id, User user)
      throws UnauthorizedException {

    SecurityChecker.getInstance().checkIfUserIsAvailable(user);
    return CrudOperations.getInstance().delete(kindName, id, user);
  }

  /**
   * Deletes all the CloudEntities specified by the List of Ids.
   *
   * @param cdl
   *          {@link EntityListDto} that contains a list of Ids to delete.
   * @return {@link EntityListDto} of a dummy {@link EntityDto}s (Endpoints
   *         requires to return any bean object).
   * @throws UnauthorizedException
   *           if the requesting {@link User} has no sufficient permission for
   *           the operation.
   */
  @ApiMethod(path = "CloudEntities/deleteAll", httpMethod = HttpMethod.POST)
  // DELETE can't have content body
  public EntityListDto deleteAll(EntityListDto cdl, User user) throws UnauthorizedException {

    SecurityChecker.getInstance().checkIfUserIsAvailable(user);
    return CrudOperations.getInstance().deleteAll(cdl, user);
  }

  /**
   * Executes a query.
   *
   * @param cbQuery
   *          {@link QueryDto} to execute.
   * @param user
   *          {@link User} who requested this operation
   * @return {@link EntityListDto} that contains the result {@link EntityDto}s.
   * @throws UnauthorizedException
   *           if the requesting {@link User} has no sufficient permission for
   *           the operation.
   * @throws BadRequestException when cbQuery has invalid members.
   */
  @ApiMethod(path = "CloudEntities/list", httpMethod = HttpMethod.POST)
  public EntityListDto list(QueryDto cbQuery, User user)
      throws UnauthorizedException, BadRequestException {

    SecurityChecker.getInstance().checkIfUserIsAvailable(user);
    try {
      return QueryOperations.getInstance().processQueryRequest(cbQuery, user);
    } catch (IllegalArgumentException e) {
      throw new BadRequestException(e.getMessage());
    }
  }
}
