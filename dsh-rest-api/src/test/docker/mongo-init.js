// Creates the user dsh-data authenticates as (dshApplicationContext.xml: <user>:<password>@dsh).
// Mounted into /docker-entrypoint-initdb.d by the http-integration-tests profile of dsh-rest-api.
db.getSiblingDB('dsh').createUser({
  user: 'dshuser',
  pwd: 'dshpass',
  roles: [{ role: 'readWrite', db: 'dsh' }]
});
