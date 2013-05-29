/*
 * Copyright (c) 2008 - 2013 10gen, Inc. <http://10gen.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.mongodb;

import org.mongodb.codecs.DocumentCodec;
import org.mongodb.codecs.PrimitiveCodecs;
import org.mongodb.command.ListDatabases;
import org.mongodb.command.Ping;
import org.mongodb.connection.BufferPool;
import org.mongodb.operation.CommandOperation;
import org.mongodb.operation.CommandResult;

import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
/**
 * Contains the commands that can be run on MongoDB that do not require a database to be selected first.  These commands
 * can be accessed via MongoClient.
 */
class ClientAdministrationImpl implements ClientAdministration {
    private static final String ADMIN_DATABASE = "admin";
    private static final Ping PING_COMMAND = new Ping();
    private static final ListDatabases LIST_DATABASES = new ListDatabases();

    private final DocumentCodec documentCodec;
    private final MongoClientImpl client;

    ClientAdministrationImpl(final MongoClientImpl client, final PrimitiveCodecs primitiveCodecs) {
        this.client = client;
        documentCodec = new DocumentCodec(primitiveCodecs);
    }

    //TODO: it's not clear from the documentation what the return type should be
    //http://docs.mongodb.org/manual/reference/command/ping/
    @Override
    public double ping() {
        final CommandResult pingResult = new CommandOperation(ADMIN_DATABASE, PING_COMMAND, documentCodec,
                getBufferPool()).execute(client.getSession());

        return (Double) pingResult.getResponse().get("ok");
    }

    @Override
    public Set<String> getDatabaseNames() {
        final CommandResult listDatabasesResult = new CommandOperation(ADMIN_DATABASE, LIST_DATABASES, documentCodec,
                getBufferPool()).execute(client.getSession());

        @SuppressWarnings("unchecked")
        final List<Document> databases = (List<Document>) listDatabasesResult.getResponse().get("databases");

        final Set<String> databaseNames = new HashSet<String>();
        for (final Document d : databases) {
            databaseNames.add(d.get("name", String.class));
        }
        return Collections.unmodifiableSet(databaseNames);
    }

    public BufferPool<ByteBuffer> getBufferPool() {
        return client.getBufferPool();
    }
}