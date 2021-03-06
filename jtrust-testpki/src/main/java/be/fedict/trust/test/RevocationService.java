/*
 * Java Trust Project.
 * Copyright (C) 2018 e-Contract.be BVBA.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License version
 * 3.0 as published by the Free Software Foundation.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, see 
 * http://www.gnu.org/licenses/.
 */

package be.fedict.trust.test;

/**
 * Interface for revocation services. A revocation service can provider
 * endpoints and generate certificate extensions.
 * 
 * @author Frank Cornelis
 *
 */
public interface RevocationService extends ExtensionProvider, EndpointProvider {

	/**
	 * Callback to receive the CA to which we belong.
	 * 
	 * @param certificationAuthority
	 */
	void setCertificationAuthority(CertificationAuthority certificationAuthority);
}
