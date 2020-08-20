/*
 *
 *  Copyright 2016 Robert Winkler
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *
 */
package io.github.resilience4j.retry.internal;

import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.test.HelloWorldException;
import io.github.resilience4j.test.HelloWorldService;
import io.vavr.control.Try;
import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;

public class RunnableRetryTest {

    private HelloWorldService helloWorldService;
    private long sleptTime = 0L;

    @Before
    public void setUp() {
        helloWorldService = mock(HelloWorldService.class);
        RetryImpl.sleepFunction = sleep -> sleptTime += sleep;
    }

    @Test
    public void shouldNotRetry() {
        Retry retryContext = Retry.ofDefaults("id");
        Runnable runnable = Retry.decorateRunnable(retryContext, helloWorldService::sayHelloWorld);

        runnable.run();

        then(helloWorldService).should().sayHelloWorld();
        assertThat(sleptTime).isEqualTo(0);
    }

    @Test
    public void testDecorateRunnable() {
        willThrow(new HelloWorldException()).given(helloWorldService).sayHelloWorld();
        Retry retry = Retry.ofDefaults("id");
        Runnable runnable = Retry.decorateRunnable(retry, helloWorldService::sayHelloWorld);

        Try<Void> result = Try.run(runnable::run);

        then(helloWorldService).should(times(3)).sayHelloWorld();
        assertThat(result.isFailure()).isTrue();
        assertThat(result.failed().get()).isInstanceOf(HelloWorldException.class);
        assertThat(sleptTime).isEqualTo(RetryConfig.DEFAULT_WAIT_DURATION * 2);
    }

    @Test
    public void testExecuteRunnable() {
        Retry retry = Retry.ofDefaults("id");

        retry.executeRunnable(helloWorldService::sayHelloWorld);

        then(helloWorldService).should().sayHelloWorld();
        assertThat(sleptTime).isEqualTo(0);
    }
}
