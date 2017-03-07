package com.buabook.kdb.query;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.buabook.kdb.connection.KdbConnection;
import com.buabook.kdb.connection.KdbProcess;
import com.buabook.kdb.data.KdbDict;
import com.buabook.kdb.exceptions.KdbTargetProcessUnavailableException;
import com.buabook.kdb.exceptions.QueryExecutionFailedException;
import com.google.common.base.Stopwatch;
import com.kx.c.KException;

/**
 * <h3>KDB Query Class - Synchronous Implementation</h3>
 * <p>Based on {@link KdbQuery} provides a synchronous (server accepts and responds 
 * immediately to query) means of communicating with a KDB process.</p> 
 * (c) 2014 - 2015 Sport Trades Ltd
 * @see KdbQuery
 * 
 * @author Jas Rajasansir
 * @version 1.0.1
 * @since 21 Jul 2014
 */
public class KdbSyncQuery extends KdbQuery {
	private static final Logger log = LoggerFactory.getLogger(KdbSyncQuery.class);

	
	public KdbSyncQuery(KdbProcess target) throws KdbTargetProcessUnavailableException {
		super(target);
	}

	public KdbSyncQuery(KdbConnection existingConnection) throws KdbTargetProcessUnavailableException {
		super(existingConnection);
	}

	
	@Override
	public Object query(String query) throws QueryExecutionFailedException {
		return query(query, null);
	}

	/**
	 * <b>NOTE</b>: This function does not perform any re-send of the query in case the connection
	 * fails (due to {@link IOException}). The calling function needs to ensure they re-send the
	 * query if they want to.
	 */
	@Override
	public Object query(String query, KdbDict arguments) throws QueryExecutionFailedException {
		if(! connection.isConnected()) {
			log.warn("Underlying connection to the kdb process ({}) has disconnected. Attempting to reconnect.", connection.getRemoteProcess());
			log.warn("NOTE: Query (and calling thread) will be pending until the process reconnects.");
			
			connection.reconnect();
		}
		
		Object queryResult = null;
		Stopwatch queryTime = null;
		
		try {
			queryTime = Stopwatch.createStarted();
			
			if(arguments == null) {
				log.debug("Running synchronous query [ Process: {} ] [ Query: {} ]", connection.getRemoteProcess(), query);
				queryResult = connection.getConnection().k(query);
			} else {
				log.debug("Running synchronous query [ Process: {} ] [ Query: {} ] [ Args: {} ]", connection.getRemoteProcess(), query, arguments);
				queryResult = connection.getConnection().k(query, arguments.convertToDict());
			}
			
			queryTime.stop();
		} catch (KException e) {
			log.error("Failed to execute synchronous query [ Process: {} ] [ Query: {} ]. Error - {}", connection.getRemoteProcess(), query, e.getMessage());
			throw new QueryExecutionFailedException(connection.getRemoteProcess().toString(), e);
		} catch (IOException e) {
			log.error("Low level I/O exception has occurred during synchronous query. Will attempt to reconnect on next query. [ Process: {} ]. Error - {}" + e.getMessage(), connection.getRemoteProcess(), e.getMessage());
			connection.disconnect();
			
			throw new QueryExecutionFailedException(connection.getRemoteProcess().toString(), e);	
		}
		
		log.debug("Query returned OK [ Process: {} ] [ Result: {} ] [ Query Time: {} ]", connection.getRemoteProcess(), queryResult, queryTime);
		
		return queryResult;
	}

}
