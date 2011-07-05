/*
 * Copyright 2009-2011, Qualcomm Innovation Center, Inc.
 * 
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 * 
 *        http://www.apache.org/licenses/LICENSE-2.0
 * 
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package org.alljoyn.bus;

/**
 * Authentication listeners are responsible for handling AllJoyn authentication
 * requests.
 */
public interface AuthListener {

    /** Authentication credentials set via authentication requests. */
    class Credentials {
        byte[] password;
        String userName;
        String certificateChain;
        String privateKey;
        byte[] logonEntry;

        Credentials() {}
    }

    /** Authentication request. */
    class AuthRequest {
        protected Credentials credentials;

        private AuthRequest() {}
    }

    /** Authentication request for a password, pincode, or passphrase. */
    class PasswordRequest extends AuthRequest {
        private boolean isNew;
        private boolean isOneTime;

        PasswordRequest(Credentials credentials, boolean isNew, boolean isOneTime) {
            this.credentials = credentials;
            this.isNew = isNew;
            this.isOneTime = isOneTime;
        }

        /**
         * Indicates request is for a newly created password.
         *
         * @return {@code true} if request is for a newly created password
         */
        public boolean isNewPassword() { 
            return isNew;
        }

        /**
         * Indicates a request is for a one time use password.
         *
         * @return {@code true} if request is for a one time use password
         */
        public boolean isOneTimePassword() {
            return isOneTime;
        }

        /**
         * Sets a requested password, pincode, or passphrase.
         *
         * @param password the password to set
         */
        public void setPassword(char[] password) {
            credentials.password = BusAttachment.encode(password);
        }
    }

    /** Authentication request for a user name. */
    class UserNameRequest extends AuthRequest {

        UserNameRequest(Credentials credentials) {
            this.credentials = credentials;
        }

        /**
         * Sets a requested user name.
         *
         * @param userName the user name to set
         */
        public void setUserName(String userName) {
            credentials.userName = userName;
        }
    }

    /** Authentication request for a chain of PEM-encoded X509 certificates. */
    class CertificateRequest extends AuthRequest {

        CertificateRequest(Credentials credentials) {
            this.credentials = credentials;
        }

        /**
         * Sets a requested public key certificate chain. The certificate must
         * be PEM encoded.
         *
         * @param certificateChain the certificate to chain to set
         */
        public void setCertificateChain(String certificateChain) {
            credentials.certificateChain = certificateChain;
        }
    }

    /** Authentication request for a PEM encoded private key. */
    class PrivateKeyRequest extends AuthRequest {

        PrivateKeyRequest(Credentials credentials) {
            this.credentials = credentials;
        }

        /**
         * Sets a requested private key. The private key must be PEM encoded and
         * may be encrypted.
         *
         * @param privateKey the private key to set
         */
        public void setPrivateKey(String privateKey) {
            credentials.privateKey = privateKey;
        }
    }

    /** Authentication request for a logon entry. */
    class LogonEntryRequest extends AuthRequest {

        LogonEntryRequest(Credentials credentials) {
            this.credentials = credentials;
        }

        /**
         * Sets a logon entry. For example for the Secure Remote
         * Password protocol in RFC 5054, a logon entry encodes the
         * N,g, s and v parameters. An SRP logon entry has the form
         * N:g:s:v where N,g,s, and v are ASCII encoded hexadecimal
         * strings and are seperated by colons.
         *
         * @param logonEntry the logon entry to set
         */
        public void setLogonEntry(char[] logonEntry) {
            credentials.logonEntry = BusAttachment.encode(logonEntry);
        }
    }

    /**
     * Authentication request for verification of a certificate chain from a
     * remote peer.
     */
    class VerifyRequest extends AuthRequest {
        private String certificateChain;

        VerifyRequest(String certificateChain) {
            this.certificateChain = certificateChain;
        }

        /**
         * Gets the PEM encoded X509 certificate chain to verify.
         *
         * @return an X509 certificate chain
         */
        public String getCertificateChain() {
            return certificateChain;
        }
    }

    /**
     * Called by an authentication mechanism making authentication requests.
     * A count allows the listener to decide whether to allow or reject mutiple
     * authentication attempts to the same peer.
     *
     * @param mechanism the name of the authentication mechanism issuing the
     *                  request
     * @param peerName  the name of the remote peer being authenticated.  On the
     *                  initiating side this will be a well-known-name for the
     *                  remote peer. On the accepting side this will be the
     *                  unique bus name for the remote peer.
     * @param count the count (starting at 1) of the number of authentication
     *              request attempts made
     * @param userName the user name for the credentials being requested.  If
     *               this is not the empty string the request is specific to the
     *               named user.
     * @param requests the requests.  The application may handle none, some, or
     *                 all of the requests.
     *
     * @return {@code true} if the request is accepted or {@code false} if the request is
     *         rejected.  If the request is rejected the authentication is
     *         complete.
     */
    boolean requested(String mechanism, String peerName, int count, String userName,
                      AuthRequest[] requests);

    /**
     * Called by the authentication engine when all authentication attempts are
     * completed.
     *
     * @param mechanism the name of the authentication mechanism that was used
     *                  or an empty string if the authentication failed
     * @param peerName  the name of the remote peer being authenticated.  On the
     *                  initiating side this will be a well-known-name for the
     *                  remote peer. On the accepting side this will be the
     *                  unique bus name for the remote peer.
     * @param authenticated {@code true} if the authentication succeeded
     */
    void completed(String mechanism, String peerName, boolean authenticated);
}
