# Tasks API Documentation

This repository contains the OpenAPI specifications for our Tasks API.

## Generate

To generate the OpenAPI documentation as a html document you can use this command:

```
openapi-generator-cli generate -g html2 -i tasks-api.yaml -o doc/html2/
```

Keep in mind that a handful of other generators are available. ([List of Generators](https://openapi-generator.tech/docs/generators/))

## Postman

To test the API, there is also a [Postman collection](./tasks-api-postman-collection.json) of all important API calls.

All you need to do is to create the following variables in Postman:

| Variable       | Remote                          | Local                                                                                               |
|----------------|---------------------------------|-----------------------------------------------------------------------------------------------------|
| API_URL_PREFIX | https://185.128.119.187         | http://localhost                                                                                   |
| JWT_TOKEN      | # the token from /users/login # | eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJ1c2VyX2lkIjoxfQ.7_mY-zTfsNlKS35U10KY_zJdNbZkR1DkhCXfJSKHRgk |
