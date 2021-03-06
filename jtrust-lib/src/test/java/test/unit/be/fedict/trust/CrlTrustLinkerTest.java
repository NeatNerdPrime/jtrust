/*
 * Java Trust Project.
 * Copyright (C) 2009 FedICT.
 * Copyright (C) 2020-2021 e-Contract.be BV.
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

package test.unit.be.fedict.trust;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import java.net.URI;
import java.security.KeyPair;
import java.security.Security;
import java.security.cert.X509CRL;
import java.security.cert.X509Certificate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Collections;
import java.util.Date;

import org.bouncycastle.asn1.x509.KeyUsage;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.easymock.EasyMock;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import be.fedict.trust.crl.CrlRepository;
import be.fedict.trust.crl.CrlTrustLinker;
import be.fedict.trust.linker.TrustLinkerResult;
import be.fedict.trust.linker.TrustLinkerResultException;
import be.fedict.trust.linker.TrustLinkerResultReason;
import be.fedict.trust.policy.DefaultAlgorithmPolicy;
import be.fedict.trust.revocation.RevocationData;
import be.fedict.trust.test.PKITestUtils;

public class CrlTrustLinkerTest {

	@BeforeEach
	public void setUp() throws Exception {
		Security.addProvider(new BouncyCastleProvider());
	}

	@Test
	public void noCrlUriInCertificate() throws Exception {
		KeyPair rootKeyPair = PKITestUtils.generateKeyPair();
		LocalDateTime notBefore = LocalDateTime.now();
		LocalDateTime notAfter = notBefore.plusMonths(1);
		X509Certificate rootCertificate = PKITestUtils.generateSelfSignedCertificate(rootKeyPair, "CN=TestRoot",
				notBefore, notAfter);

		KeyPair keyPair = PKITestUtils.generateKeyPair();
		X509Certificate certificate = PKITestUtils.generateCertificate(keyPair.getPublic(), "CN=Test", notBefore,
				notAfter, rootCertificate, rootKeyPair.getPrivate());

		CrlRepository mockCrlRepository = EasyMock.createMock(CrlRepository.class);

		EasyMock.replay(mockCrlRepository);

		CrlTrustLinker crlTrustLinker = new CrlTrustLinker(mockCrlRepository);
		TrustLinkerResult result = crlTrustLinker.hasTrustLink(certificate, rootCertificate, null, new RevocationData(),
				new DefaultAlgorithmPolicy());

		EasyMock.verify(mockCrlRepository);
		assertEquals(TrustLinkerResult.UNDECIDED, result);
	}

	@Test
	public void invalidCrlUriInCertificate() throws Exception {
		KeyPair rootKeyPair = PKITestUtils.generateKeyPair();
		LocalDateTime notBefore = LocalDateTime.now();
		LocalDateTime notAfter = notBefore.plusMonths(1);
		X509Certificate rootCertificate = PKITestUtils.generateSelfSignedCertificate(rootKeyPair, "CN=TestRoot",
				notBefore, notAfter, true, 0);

		KeyPair keyPair = PKITestUtils.generateKeyPair();
		X509Certificate certificate = PKITestUtils.generateCertificate(keyPair.getPublic(), "CN=Test", notBefore,
				notAfter, rootCertificate, rootKeyPair.getPrivate(), false, -1, "foobar");

		CrlRepository mockCrlRepository = EasyMock.createMock(CrlRepository.class);

		EasyMock.replay(mockCrlRepository);

		CrlTrustLinker crlTrustLinker = new CrlTrustLinker(mockCrlRepository);

		TrustLinkerResult result = crlTrustLinker.hasTrustLink(certificate, rootCertificate, null, new RevocationData(),
				new DefaultAlgorithmPolicy());
		assertEquals(TrustLinkerResult.UNDECIDED, result);
	}

	@Test
	public void noEntryInCrlRepository() throws Exception {
		KeyPair rootKeyPair = PKITestUtils.generateKeyPair();
		LocalDateTime notBefore = LocalDateTime.now();
		LocalDateTime notAfter = notBefore.plusMonths(1);
		X509Certificate rootCertificate = PKITestUtils.generateSelfSignedCertificate(rootKeyPair, "CN=TestRoot",
				notBefore, notAfter, true, 0);

		KeyPair keyPair = PKITestUtils.generateKeyPair();
		X509Certificate certificate = PKITestUtils.generateCertificate(keyPair.getPublic(), "CN=Test", notBefore,
				notAfter, rootCertificate, rootKeyPair.getPrivate(), false, -1, "http://crl-uri");

		Date validationDate = new Date();

		CrlRepository mockCrlRepository = EasyMock.createMock(CrlRepository.class);
		EasyMock.expect(mockCrlRepository.findCrl(new URI("http://crl-uri"), rootCertificate, validationDate))
				.andReturn(null);

		EasyMock.replay(mockCrlRepository);

		CrlTrustLinker crlTrustLinker = new CrlTrustLinker(mockCrlRepository);

		TrustLinkerResult result = crlTrustLinker.hasTrustLink(certificate, rootCertificate, validationDate,
				new RevocationData(), new DefaultAlgorithmPolicy());

		assertEquals(TrustLinkerResult.UNDECIDED, result);
		EasyMock.verify(mockCrlRepository);
	}

	@Test
	public void emptyCrlPasses() throws Exception {
		KeyPair rootKeyPair = PKITestUtils.generateKeyPair();
		LocalDateTime notBefore = LocalDateTime.now();
		LocalDateTime notAfter = notBefore.plusMonths(1);
		X509Certificate rootCertificate = PKITestUtils.generateSelfSignedCertificate(rootKeyPair, "CN=TestRoot",
				notBefore, notAfter, true, 0, null, new KeyUsage(KeyUsage.cRLSign));

		KeyPair keyPair = PKITestUtils.generateKeyPair();
		X509Certificate certificate = PKITestUtils.generateCertificate(keyPair.getPublic(), "CN=Test", notBefore,
				notAfter, rootCertificate, rootKeyPair.getPrivate(), false, -1, "http://crl-uri");

		Date validationDate = Date.from(notBefore.plusDays(1).atZone(ZoneId.systemDefault()).toInstant());

		CrlRepository mockCrlRepository = EasyMock.createMock(CrlRepository.class);
		X509CRL x509crl = PKITestUtils.generateCrl(rootKeyPair.getPrivate(), rootCertificate, notBefore, notAfter);
		EasyMock.expect(mockCrlRepository.findCrl(new URI("http://crl-uri"), rootCertificate, validationDate))
				.andReturn(x509crl);

		EasyMock.replay(mockCrlRepository);

		CrlTrustLinker crlTrustLinker = new CrlTrustLinker(mockCrlRepository);

		TrustLinkerResult result = crlTrustLinker.hasTrustLink(certificate, rootCertificate, validationDate,
				new RevocationData(), new DefaultAlgorithmPolicy());

		assertEquals(TrustLinkerResult.TRUSTED, result);
		EasyMock.verify(mockCrlRepository);
	}

	@Test
	public void crlMissingKeyUsage() throws Exception {
		KeyPair rootKeyPair = PKITestUtils.generateKeyPair();
		LocalDateTime notBefore = LocalDateTime.now();
		LocalDateTime notAfter = notBefore.plusMonths(1);
		X509Certificate rootCertificate = PKITestUtils.generateSelfSignedCertificate(rootKeyPair, "CN=TestRoot",
				notBefore, notAfter, true, 0);

		KeyPair keyPair = PKITestUtils.generateKeyPair();
		X509Certificate certificate = PKITestUtils.generateCertificate(keyPair.getPublic(), "CN=Test", notBefore,
				notAfter, rootCertificate, rootKeyPair.getPrivate(), false, -1, "https://crl-uri");

		Date validationDate = Date.from(notBefore.plusDays(1).atZone(ZoneId.systemDefault()).toInstant());

		CrlRepository mockCrlRepository = EasyMock.createMock(CrlRepository.class);
		X509CRL x509crl = PKITestUtils.generateCrl(rootKeyPair.getPrivate(), rootCertificate, notBefore, notAfter);
		EasyMock.expect(mockCrlRepository.findCrl(new URI("https://crl-uri"), rootCertificate, validationDate))
				.andReturn(x509crl);

		EasyMock.replay(mockCrlRepository);

		CrlTrustLinker crlTrustLinker = new CrlTrustLinker(mockCrlRepository);

		TrustLinkerResult result = crlTrustLinker.hasTrustLink(certificate, rootCertificate, validationDate,
				new RevocationData(), new DefaultAlgorithmPolicy());

		assertEquals(TrustLinkerResult.TRUSTED, result);
		EasyMock.verify(mockCrlRepository);
	}

	@Test
	public void crlMissingCRLSignKeyUsage() throws Exception {
		KeyPair rootKeyPair = PKITestUtils.generateKeyPair();
		LocalDateTime notBefore = LocalDateTime.now();
		LocalDateTime notAfter = notBefore.plusMonths(1);
		X509Certificate rootCertificate = PKITestUtils.generateSelfSignedCertificate(rootKeyPair, "CN=TestRoot",
				notBefore, notAfter, true, 0, null, new KeyUsage(KeyUsage.dataEncipherment));

		KeyPair keyPair = PKITestUtils.generateKeyPair();
		X509Certificate certificate = PKITestUtils.generateCertificate(keyPair.getPublic(), "CN=Test", notBefore,
				notAfter, rootCertificate, rootKeyPair.getPrivate(), false, -1, "https://crl-uri");

		Date validationDate = Date.from(notBefore.plusDays(1).atZone(ZoneId.systemDefault()).toInstant());

		CrlRepository mockCrlRepository = EasyMock.createMock(CrlRepository.class);
		X509CRL x509crl = PKITestUtils.generateCrl(rootKeyPair.getPrivate(), rootCertificate, notBefore, notAfter);
		EasyMock.expect(mockCrlRepository.findCrl(new URI("https://crl-uri"), rootCertificate, validationDate))
				.andReturn(x509crl);

		EasyMock.replay(mockCrlRepository);

		CrlTrustLinker crlTrustLinker = new CrlTrustLinker(mockCrlRepository);

		TrustLinkerResult result = crlTrustLinker.hasTrustLink(certificate, rootCertificate, validationDate,
				new RevocationData(), new DefaultAlgorithmPolicy());

		assertEquals(TrustLinkerResult.UNDECIDED, result);
		EasyMock.verify(mockCrlRepository);
	}

	@Test
	public void oldCrl() throws Exception {
		KeyPair rootKeyPair = PKITestUtils.generateKeyPair();
		LocalDateTime notBefore = LocalDateTime.now();
		LocalDateTime notAfter = notBefore.plusMonths(1);
		X509Certificate rootCertificate = PKITestUtils.generateSelfSignedCertificate(rootKeyPair, "CN=TestRoot",
				notBefore, notAfter, true, 0);

		KeyPair keyPair = PKITestUtils.generateKeyPair();
		X509Certificate certificate = PKITestUtils.generateCertificate(keyPair.getPublic(), "CN=Test", notBefore,
				notAfter, rootCertificate, rootKeyPair.getPrivate(), false, -1, "http://crl-uri");

		Date validationDate = Date.from(notBefore.plusDays(1).atZone(ZoneId.systemDefault()).toInstant());

		CrlRepository mockCrlRepository = EasyMock.createMock(CrlRepository.class);
		X509CRL x509crl = PKITestUtils.generateCrl(rootKeyPair.getPrivate(), rootCertificate, notBefore.minusMonths(2),
				notAfter.minusMonths(2));
		EasyMock.expect(mockCrlRepository.findCrl(new URI("http://crl-uri"), rootCertificate, validationDate))
				.andReturn(x509crl);

		EasyMock.replay(mockCrlRepository);

		CrlTrustLinker crlTrustLinker = new CrlTrustLinker(mockCrlRepository);

		TrustLinkerResult result = crlTrustLinker.hasTrustLink(certificate, rootCertificate, validationDate,
				new RevocationData(), new DefaultAlgorithmPolicy());

		assertEquals(TrustLinkerResult.UNDECIDED, result);
		EasyMock.verify(mockCrlRepository);
	}

	@Test
	public void crlNotIssuedByRoot() throws Exception {
		KeyPair rootKeyPair = PKITestUtils.generateKeyPair();
		LocalDateTime notBefore = LocalDateTime.now();
		LocalDateTime notAfter = notBefore.plusMonths(1);
		X509Certificate rootCertificate = PKITestUtils.generateSelfSignedCertificate(rootKeyPair, "CN=TestRoot",
				notBefore, notAfter, true, 0);

		KeyPair keyPair = PKITestUtils.generateKeyPair();
		X509Certificate certificate = PKITestUtils.generateCertificate(keyPair.getPublic(), "CN=Test", notBefore,
				notAfter, rootCertificate, rootKeyPair.getPrivate(), false, -1, "http://crl-uri");

		Date validationDate = Date.from(notBefore.plusDays(1).atZone(ZoneId.systemDefault()).toInstant());

		CrlRepository mockCrlRepository = EasyMock.createMock(CrlRepository.class);
		X509CRL x509crl = PKITestUtils.generateCrl(rootKeyPair.getPrivate(), certificate, notBefore, notAfter);
		EasyMock.expect(mockCrlRepository.findCrl(new URI("http://crl-uri"), rootCertificate, validationDate))
				.andReturn(x509crl);

		EasyMock.replay(mockCrlRepository);

		CrlTrustLinker crlTrustLinker = new CrlTrustLinker(mockCrlRepository);

		TrustLinkerResult result = crlTrustLinker.hasTrustLink(certificate, rootCertificate, validationDate,
				new RevocationData(), new DefaultAlgorithmPolicy());

		assertEquals(TrustLinkerResult.UNDECIDED, result);
		EasyMock.verify(mockCrlRepository);
	}

	@Test
	public void crlNotSignedByRoot() throws Exception {
		KeyPair rootKeyPair = PKITestUtils.generateKeyPair();
		LocalDateTime notBefore = LocalDateTime.now();
		LocalDateTime notAfter = notBefore.plusMonths(1);
		X509Certificate rootCertificate = PKITestUtils.generateSelfSignedCertificate(rootKeyPair, "CN=TestRoot",
				notBefore, notAfter, true, 0);

		KeyPair keyPair = PKITestUtils.generateKeyPair();
		X509Certificate certificate = PKITestUtils.generateCertificate(keyPair.getPublic(), "CN=Test", notBefore,
				notAfter, rootCertificate, rootKeyPair.getPrivate(), false, -1, "http://crl-uri");

		Date validationDate = Date.from(notBefore.plusDays(1).atZone(ZoneId.systemDefault()).toInstant());

		CrlRepository mockCrlRepository = EasyMock.createMock(CrlRepository.class);
		X509CRL x509crl = PKITestUtils.generateCrl(keyPair.getPrivate(), rootCertificate, notBefore, notAfter);
		EasyMock.expect(mockCrlRepository.findCrl(new URI("http://crl-uri"), rootCertificate, validationDate))
				.andReturn(x509crl);

		EasyMock.replay(mockCrlRepository);

		CrlTrustLinker crlTrustLinker = new CrlTrustLinker(mockCrlRepository);

		TrustLinkerResult result = crlTrustLinker.hasTrustLink(certificate, rootCertificate, validationDate,
				new RevocationData(), new DefaultAlgorithmPolicy());

		assertEquals(TrustLinkerResult.UNDECIDED, result);
		EasyMock.verify(mockCrlRepository);
	}

	@Test
	public void tooFreshCrl() throws Exception {
		KeyPair rootKeyPair = PKITestUtils.generateKeyPair();
		LocalDateTime notBefore = LocalDateTime.now();
		LocalDateTime notAfter = notBefore.plusMonths(1);
		X509Certificate rootCertificate = PKITestUtils.generateSelfSignedCertificate(rootKeyPair, "CN=TestRoot",
				notBefore, notAfter, true, 0);

		Date validationDate = Date.from(notBefore.plusDays(1).atZone(ZoneId.systemDefault()).toInstant());

		KeyPair keyPair = PKITestUtils.generateKeyPair();
		X509Certificate certificate = PKITestUtils.generateCertificate(keyPair.getPublic(), "CN=Test", notBefore,
				notAfter, rootCertificate, rootKeyPair.getPrivate(), false, -1, "https://crl-uri");

		CrlRepository mockCrlRepository = EasyMock.createMock(CrlRepository.class);
		X509CRL x509crl = PKITestUtils.generateCrl(rootKeyPair.getPrivate(), rootCertificate, notBefore.plusMonths(2),
				notAfter.plusMonths(2));
		EasyMock.expect(mockCrlRepository.findCrl(new URI("https://crl-uri"), rootCertificate, validationDate))
				.andReturn(x509crl);

		EasyMock.replay(mockCrlRepository);

		CrlTrustLinker crlTrustLinker = new CrlTrustLinker(mockCrlRepository);

		TrustLinkerResult result = crlTrustLinker.hasTrustLink(certificate, rootCertificate, validationDate,
				new RevocationData(), new DefaultAlgorithmPolicy());

		assertEquals(TrustLinkerResult.UNDECIDED, result);
		EasyMock.verify(mockCrlRepository);
	}

	@Test
	public void revokedCertificate() throws Exception {
		KeyPair rootKeyPair = PKITestUtils.generateKeyPair();
		LocalDateTime notBefore = LocalDateTime.now();
		LocalDateTime notAfter = notBefore.plusMonths(1);
		X509Certificate rootCertificate = PKITestUtils.generateSelfSignedCertificate(rootKeyPair, "CN=TestRoot",
				notBefore, notAfter, true, 0, null, new KeyUsage(KeyUsage.cRLSign));

		KeyPair keyPair = PKITestUtils.generateKeyPair();
		X509Certificate certificate = PKITestUtils.generateCertificate(keyPair.getPublic(), "CN=Test", notBefore,
				notAfter, rootCertificate, rootKeyPair.getPrivate(), false, -1, "http://crl-uri");

		Date validationDate = Date.from(notBefore.plusDays(1).atZone(ZoneId.systemDefault()).toInstant());

		CrlRepository mockCrlRepository = EasyMock.createMock(CrlRepository.class);
		X509CRL x509crl = PKITestUtils.generateCrl(rootKeyPair.getPrivate(), rootCertificate, notBefore, notAfter,
				certificate.getSerialNumber());
		EasyMock.expect(mockCrlRepository.findCrl(new URI("http://crl-uri"), rootCertificate, validationDate))
				.andReturn(x509crl);

		EasyMock.replay(mockCrlRepository);

		CrlTrustLinker crlTrustLinker = new CrlTrustLinker(mockCrlRepository);

		try {
			crlTrustLinker.hasTrustLink(certificate, rootCertificate, validationDate, new RevocationData(),
					new DefaultAlgorithmPolicy());
			fail();
		} catch (TrustLinkerResultException e) {
			assertEquals(TrustLinkerResultReason.INVALID_REVOCATION_STATUS, e.getReason());
		}

		EasyMock.verify(mockCrlRepository);
	}

	@Test
	public void crlMD5Signature() throws Exception {
		KeyPair rootKeyPair = PKITestUtils.generateKeyPair();
		LocalDateTime notBefore = LocalDateTime.now();
		LocalDateTime notAfter = notBefore.plusMonths(1);
		X509Certificate rootCertificate = PKITestUtils.generateSelfSignedCertificate(rootKeyPair, "CN=TestRoot",
				notBefore, notAfter, true, 0, null, new KeyUsage(KeyUsage.cRLSign));

		KeyPair keyPair = PKITestUtils.generateKeyPair();
		X509Certificate certificate = PKITestUtils.generateCertificate(keyPair.getPublic(), "CN=Test", notBefore,
				notAfter, rootCertificate, rootKeyPair.getPrivate(), false, -1, "https://crl-uri");

		Date validationDate = Date.from(notBefore.plusDays(1).atZone(ZoneId.systemDefault()).toInstant());

		CrlRepository mockCrlRepository = EasyMock.createMock(CrlRepository.class);
		X509CRL x509crl = PKITestUtils.generateCrl(rootKeyPair.getPrivate(), rootCertificate, notBefore, notAfter,
				"MD5withRSA", certificate.getSerialNumber());
		EasyMock.expect(mockCrlRepository.findCrl(new URI("https://crl-uri"), rootCertificate, validationDate))
				.andReturn(x509crl);

		EasyMock.replay(mockCrlRepository);

		CrlTrustLinker crlTrustLinker = new CrlTrustLinker(mockCrlRepository);

		try {
			crlTrustLinker.hasTrustLink(certificate, rootCertificate, validationDate, new RevocationData(),
					new DefaultAlgorithmPolicy());
			fail();
		} catch (TrustLinkerResultException e) {
			assertEquals(TrustLinkerResultReason.INVALID_ALGORITHM, e.getReason());
		}

		EasyMock.verify(mockCrlRepository);
	}

	@Test
	public void futureRevokedCertificate() throws Exception {
		KeyPair rootKeyPair = PKITestUtils.generateKeyPair();
		LocalDateTime notBefore = LocalDateTime.now();
		LocalDateTime notAfter = notBefore.plusMonths(1);
		X509Certificate rootCertificate = PKITestUtils.generateSelfSignedCertificate(rootKeyPair, "CN=TestRoot",
				notBefore, notAfter, true, 0, null, new KeyUsage(KeyUsage.cRLSign));

		KeyPair keyPair = PKITestUtils.generateKeyPair();
		X509Certificate certificate = PKITestUtils.generateCertificate(keyPair.getPublic(), "CN=Test", notBefore,
				notAfter, rootCertificate, rootKeyPair.getPrivate(), false, -1, "http://crl-uri");

		Date validationDate = Date.from(notBefore.plusDays(1).atZone(ZoneId.systemDefault()).toInstant());

		CrlRepository mockCrlRepository = EasyMock.createMock(CrlRepository.class);
		X509CRL x509crl = PKITestUtils.generateCrl(rootKeyPair.getPrivate(), rootCertificate, notBefore, notAfter,
				Collections.singletonList(
						new PKITestUtils.RevokedCertificate(certificate.getSerialNumber(), notBefore.plusDays(2))));
		EasyMock.expect(mockCrlRepository.findCrl(new URI("http://crl-uri"), rootCertificate, validationDate))
				.andReturn(x509crl);

		EasyMock.replay(mockCrlRepository);

		CrlTrustLinker crlTrustLinker = new CrlTrustLinker(mockCrlRepository);

		TrustLinkerResult result = crlTrustLinker.hasTrustLink(certificate, rootCertificate, validationDate,
				new RevocationData(), new DefaultAlgorithmPolicy());

		assertEquals(TrustLinkerResult.TRUSTED, result);
		EasyMock.verify(mockCrlRepository);
	}

	@Test
	public void testNullX509CRL() throws Exception {
		final KeyPair rootKeyPair = PKITestUtils.generateKeyPair();
		LocalDateTime notBefore = LocalDateTime.now();
		LocalDateTime notAfter = notBefore.plusMonths(1);
		final X509Certificate rootCertificate = PKITestUtils.generateSelfSignedCertificate(rootKeyPair, "CN=TestRoot",
				notBefore, notAfter, true, 0, null, new KeyUsage(KeyUsage.cRLSign));

		final KeyPair keyPair = PKITestUtils.generateKeyPair();
		final X509Certificate certificate = PKITestUtils.generateCertificate(keyPair.getPublic(), "CN=Test", notBefore,
				notAfter, rootCertificate, rootKeyPair.getPrivate(), false, -1, "http://crl-uri");

		final Date validationDate = Date.from(notBefore.plusDays(1).atZone(ZoneId.systemDefault()).toInstant());

		final CrlRepository mockCrlRepository = EasyMock.createMock(CrlRepository.class);
		EasyMock.expect(mockCrlRepository.findCrl(new URI("http://crl-uri"), rootCertificate, validationDate))
				.andReturn(null);

		EasyMock.replay(mockCrlRepository);

		final CrlTrustLinker crlTrustLinker = new CrlTrustLinker(mockCrlRepository);
		final TrustLinkerResult result = crlTrustLinker.hasTrustLink(certificate, rootCertificate, validationDate,
				new RevocationData(), new DefaultAlgorithmPolicy());

		EasyMock.verify(mockCrlRepository);

		assertEquals(TrustLinkerResult.UNDECIDED, result);
	}
}
