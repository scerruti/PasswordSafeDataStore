package com.otabi;

import com.google.api.client.util.IOUtils;
import com.google.api.client.util.Preconditions;
import com.google.api.client.util.store.AbstractDataStore;
import com.google.api.client.util.store.AbstractDataStoreFactory;
import com.google.api.client.util.store.DataStore;
import com.intellij.credentialStore.CredentialAttributes;
import com.intellij.credentialStore.Credentials;
import com.intellij.ide.passwordSafe.PasswordSafe;
import com.intellij.openapi.diagnostic.Logger;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.Serializable;
import java.util.Base64;
import java.util.Collection;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

@SuppressWarnings("unused")
public class PasswordSafeDataStoreFactory extends AbstractDataStoreFactory {

    private static final Logger LOGGER = Logger
            .getInstance(PasswordSafeDataStoreFactory.class.getName());

    private static final PasswordSafe safe = PasswordSafe.getInstance();

    @Override
    protected <V extends Serializable> DataStore<V> createDataStore(
            String id) {
        return new PasswordSafeDataStore<>(this, safe, id);
    }

    /**
     * @param <V> serializable type of the mapped value
     */
    static class PasswordSafeDataStore<V extends Serializable> extends AbstractDataStore<V> {
        private static final String PASSWORD_SAFE_DATA_STORE_SERVICE = "PasswordSafeDataStoreService";
        private final Lock lock = new ReentrantLock();
        private final PasswordSafe safe;
        private final static Base64.Encoder encoder = Base64.getEncoder();
        private final static Base64.Decoder decoder = Base64.getDecoder();

        PasswordSafeDataStore(PasswordSafeDataStoreFactory dataStore, PasswordSafe safe, String id) {
            super(dataStore, id);
            this.safe = safe;
        }

        @Override
        public Set<String> keySet() {
            LOGGER.warn("Unimplemented method PasswordSafeDataStore.keySet called.");
            return null;
        }

        public final Collection<V> values() {
            LOGGER.warn("Unimplemented method PasswordSafeDataStore.values called.");
            return null;
        }

        public final V get(String key) throws IOException {
            if (key == null) {
                return null;
            }
            lock.lock();
            try {
                CredentialAttributes attributes = getCredentialAttributes(key);
                Credentials credentials = safe.get(attributes);
                if (credentials == null || credentials.getPasswordAsString() == null) {
                    return null;
                } else {
                    return (IOUtils.deserialize(decoder.decode(credentials.getPasswordAsString().getBytes())));
                }
            } finally {
                lock.unlock();
            }
        }

        public final DataStore<V> set(String key, V value) throws IOException {
            Preconditions.checkNotNull(key);
            Preconditions.checkNotNull(value);
            lock.lock();
            try {
                CredentialAttributes attributes = getCredentialAttributes(key);
                Credentials credentials = new Credentials(key, new String(encoder.encode(IOUtils.serialize(value))));
                safe.set(attributes, credentials);
            } finally {
                lock.unlock();
            }
            return this;
        }

        @NotNull
        private CredentialAttributes getCredentialAttributes(String key) {
            String service = PASSWORD_SAFE_DATA_STORE_SERVICE+"."+this.getId();
            return new CredentialAttributes(service, key);
        }

        @Override
        public DataStore<V> clear() {
            LOGGER.warn("Unimplemented method PasswordSafeDataStore.clear called.");
            return null;
        }

        @Override
        public DataStore<V> delete(String key) {
            LOGGER.warn("Unimplemented method PasswordSafeDataStore.delete called.");
            return null;
        }

        @Override
        public PasswordSafeDataStoreFactory getDataStoreFactory() {
            return (PasswordSafeDataStoreFactory) super.getDataStoreFactory();
        }

    }
}
