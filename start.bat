
java -jar sc-rest-0.7.0.jar --server.port=8000 --logging.level.org.conceptoriented.sc.rest=DEBUG --logging.file=dc.log --server.session.timeout=10800

rem How long the server will keep an inactive session
rem 3 hours
rem --server.session.timeout=10800

rem How long the browser will keep the session cookie. Default is -1 (infinitely)
rem 12 hours
rem --server.session.cookie.max-age=43200
