/*
 * Copyright (C) 2015 Square, Inc.
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
package com.jakewharton.retrofit2.adapter.rxjava2;

import io.reactivex.Single;
import io.reactivex.observers.TestObserver;
import java.io.IOException;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.http.GET;

import static com.google.common.truth.Truth.assertThat;
import static okhttp3.mockwebserver.SocketPolicy.DISCONNECT_AFTER_REQUEST;

public final class SingleTest {
  @Rule public final MockWebServer server = new MockWebServer();

  interface Service {
    @GET("/") Single<String> body();
    @GET("/") Single<Response<String>> response();
    @GET("/") Single<Result<String>> result();
  }

  private Service service;

  @Before public void setUp() {
    Retrofit retrofit = new Retrofit.Builder()
        .baseUrl(server.url("/"))
        .addConverterFactory(new StringConverterFactory())
        .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
        .build();
    service = retrofit.create(Service.class);
  }

  @Test public void bodySuccess200() {
    server.enqueue(new MockResponse().setBody("Hi"));

    TestObserver<String> observer = new TestObserver<>();
    service.body().subscribe(observer);
    observer.assertValues("Hi");
  }

  @Test public void bodySuccess404() {
    server.enqueue(new MockResponse().setResponseCode(404));

    TestObserver<String> observer = new TestObserver<>();
    service.body().subscribe(observer);
    observer.assertFailureAndMessage(HttpException.class, "HTTP 404 Client Error");
  }

  @Test public void bodyFailure() {
    server.enqueue(new MockResponse().setSocketPolicy(DISCONNECT_AFTER_REQUEST));

    TestObserver<String> observer = new TestObserver<>();
    service.body().subscribe(observer);
    observer.assertError(IOException.class);
  }

  @Test public void responseSuccess200() {
    server.enqueue(new MockResponse().setBody("Hi"));

    TestObserver<Response<String>> observer = new TestObserver<>();
    service.response().subscribe(observer);
    Response<String> response = observer.values().get(0);
    assertThat(response.isSuccessful()).isTrue();
    assertThat(response.body()).isEqualTo("Hi");
  }

  @Test public void responseSuccess404() throws IOException {
    server.enqueue(new MockResponse().setResponseCode(404).setBody("Hi"));

    TestObserver<Response<String>> observer = new TestObserver<>();
    service.response().subscribe(observer);
    Response<String> response = observer.values().get(0);
    assertThat(response.isSuccessful()).isFalse();
    assertThat(response.errorBody().string()).isEqualTo("Hi");
  }

  @Test public void responseFailure() {
    server.enqueue(new MockResponse().setSocketPolicy(DISCONNECT_AFTER_REQUEST));

    TestObserver<Response<String>> observer = new TestObserver<>();
    service.response().subscribe(observer);
    observer.assertError(IOException.class);
  }

  @Test public void resultSuccess200() {
    server.enqueue(new MockResponse().setBody("Hi"));

    TestObserver<Result<String>> observer = new TestObserver<>();
    service.result().subscribe(observer);
    Result<String> result = observer.values().get(0);
    assertThat(result.isError()).isFalse();
    Response<String> response = result.response();
    assertThat(response.isSuccessful()).isTrue();
    assertThat(response.body()).isEqualTo("Hi");
  }

  @Test public void resultSuccess404() throws IOException {
    server.enqueue(new MockResponse().setResponseCode(404).setBody("Hi"));

    TestObserver<Result<String>> observer = new TestObserver<>();
    service.result().subscribe(observer);
    Result<String> result = observer.values().get(0);
    assertThat(result.isError()).isFalse();
    Response<String> response = result.response();
    assertThat(response.isSuccessful()).isFalse();
    assertThat(response.errorBody().string()).isEqualTo("Hi");
  }

  @Test public void resultFailure() {
    server.enqueue(new MockResponse().setSocketPolicy(DISCONNECT_AFTER_REQUEST));

    TestObserver<Result<String>> observer = new TestObserver<>();
    service.result().subscribe(observer);
    Result<String> result = observer.values().get(0);
    assertThat(result.isError()).isTrue();
    assertThat(result.error()).isInstanceOf(IOException.class);
  }
}
