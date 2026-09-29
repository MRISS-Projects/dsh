// Creates the user dsh-data authenticates as: the user and password of the connection string in
// dshApplicationContext.xml, with /dsh as the authentication database.
// Mounted into /docker-entrypoint-initdb.d by the http-integration-tests profile of dsh-rest-api.
// The credentials must match that profile's --mongo.user and --mongo.password arguments.
db.getSiblingDB('dsh').createUser({
  user: 'dshuser',
  pwd: 'dshpass',
  roles: [{ role: 'readWrite', db: 'dsh' }]
});
