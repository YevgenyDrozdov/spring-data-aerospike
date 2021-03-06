/*
 * Copyright 2012-2018 the original author or authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.data.aerospike.core;

import com.aerospike.client.AerospikeException;
import com.aerospike.client.ResultCode;
import org.junit.Test;
import org.springframework.dao.*;
import org.springframework.data.keyvalue.core.UncategorizedKeyValueException;

import static org.assertj.core.api.Assertions.assertThat;

public class DefaultAerospikeExceptionTranslatorTest {

    private DefaultAerospikeExceptionTranslator translator = new DefaultAerospikeExceptionTranslator();

    @Test
    public void shouldTranslateKeyExistError() {
        AerospikeException cause = new AerospikeException(ResultCode.KEY_EXISTS_ERROR);
        DataAccessException actual = translator.translateExceptionIfPossible(cause);
        assertThat(actual).isExactlyInstanceOf(DuplicateKeyException.class);
    }

    @Test
    public void shouldTranslateKeyNonFoundError() {
        AerospikeException cause = new AerospikeException(ResultCode.KEY_NOT_FOUND_ERROR);
        DataAccessException actual = translator.translateExceptionIfPossible(cause);
        assertThat(actual).isExactlyInstanceOf(DataRetrievalFailureException.class);
    }

    @Test
    public void shouldTranslateTimeoutError() {
        AerospikeException cause = new AerospikeException(ResultCode.TIMEOUT);
        DataAccessException actual = translator.translateExceptionIfPossible(cause);
        assertThat(actual).isExactlyInstanceOf(QueryTimeoutException.class);
    }

    @Test
    public void shouldTranslateQueryTimeoutError() {
        AerospikeException cause = new AerospikeException(ResultCode.QUERY_TIMEOUT);
        DataAccessException actual = translator.translateExceptionIfPossible(cause);
        assertThat(actual).isExactlyInstanceOf(QueryTimeoutException.class);
    }

    @Test
    public void shouldTranslateQueryDeviceOverloadError() {
        AerospikeException cause = new AerospikeException(ResultCode.DEVICE_OVERLOAD);
        DataAccessException actual = translator.translateExceptionIfPossible(cause);
        assertThat(actual).isExactlyInstanceOf(TransientDataAccessResourceException.class);
    }

    @Test
    public void shouldTranslateQueryNoMoreConnectionsError() {
        AerospikeException cause = new AerospikeException(ResultCode.NO_MORE_CONNECTIONS);
        DataAccessException actual = translator.translateExceptionIfPossible(cause);
        assertThat(actual).isExactlyInstanceOf(TransientDataAccessResourceException.class);
    }

    @Test
    public void shouldTranslateQueryKeyBusyError() {
        AerospikeException cause = new AerospikeException(ResultCode.KEY_BUSY);
        DataAccessException actual = translator.translateExceptionIfPossible(cause);
        assertThat(actual).isExactlyInstanceOf(TransientDataAccessResourceException.class);
    }

    @Test
    public void shouldTranslateAerospikeError() {
        AerospikeException cause = new AerospikeException("any");
        DataAccessException actual = translator.translateExceptionIfPossible(cause);
        assertThat(actual).isExactlyInstanceOf(RecoverableDataAccessException.class);
    }

    @Test
    public void shouldNotTranslateUnknownError() {
        RuntimeException cause = new RuntimeException();
        DataAccessException actual = translator.translateExceptionIfPossible(cause);
        assertThat(actual).isNull();
    }
}
