package com.github.marschall.siteawareldapsprovider;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Hashtable;
import java.util.List;

import javax.naming.Context;
import javax.naming.NamingException;
import javax.naming.directory.BasicAttribute;
import javax.naming.directory.BasicAttributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import javax.naming.ldap.spi.LdapDnsProviderResult;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import com.github.marschall.siteawareldapsprovider.SiteAwareLdapsProvider.DnsRecord;

class SiteAwareLdapsProviderTest {

  @Test
  void toServiceName() throws NamingException {
    assertEquals("_ldap._tcp.example.com", SiteAwareLdapsProvider.toServiceName("ldaps:///dc=example,dc=com"));
  }

  @Test
  void readFromContext() throws NamingException {
    BasicAttributes attributes = new BasicAttributes(true);
    BasicAttribute srvAttribute = new BasicAttribute("SRV");

    // priority weight port target.
    srvAttribute.add("10 1 389 node1.example.com.");
    srvAttribute.add("10 2 389 node2.example.com.");
    srvAttribute.add("20 1 389 node3.example.com.");
    srvAttribute.add("30 1 389 node4.example.com.");
    attributes.put(srvAttribute);

    String serviceName = "_ldap._tcp.example.com";
    DirContext mockedContext = mock(DirContext.class);

    when(mockedContext.getAttributes(serviceName, new String[] { "SRV" }))
      .thenReturn(attributes);

    List<DnsRecord> records = SiteAwareLdapsProvider.readFromContext(mockedContext, serviceName);
    assertEquals(4, records.size());

    DnsRecord record = records.get(0);
    assertEquals(10, record.getPriority());
    assertEquals(1, record.getWeight());
    assertEquals(389, record.getPort());
    assertEquals("node1.example.com", record.getHost());

    record = records.get(1);
    assertEquals(10, record.getPriority());
    assertEquals(2, record.getWeight());
    assertEquals(389, record.getPort());
    assertEquals("node2.example.com", record.getHost());

    record = records.get(2);
    assertEquals(20, record.getPriority());
    assertEquals(1, record.getWeight());
    assertEquals(389, record.getPort());
    assertEquals("node3.example.com", record.getHost());

    record = records.get(3);
    assertEquals(30, record.getPriority());
    assertEquals(1, record.getWeight());
    assertEquals(389, record.getPort());
    assertEquals("node4.example.com", record.getHost());
  }

  @Test
  void toDnsProviderResult() throws NamingException {
    List<DnsRecord> records = List.of(
            DnsRecord.fromString("10 1 389 node1.example.com."),
            DnsRecord.fromString("10 2 389 node2.example.com."),
            DnsRecord.fromString("20 1 389 node3.example.com."),
            DnsRecord.fromString("30 1 389 node4.example.com."));

    String domainName = "example.com";
    LdapDnsProviderResult providerResult = SiteAwareLdapsProvider.toDnsProviderResult(domainName, records);
    assertEquals(domainName, providerResult.getDomainName());
    List<String> endpoints = providerResult.getEndpoints();

    assertEquals(2, endpoints.size());
    String endpoint = endpoints.get(0);
    assertEquals("ldaps://node1.example.com:636/dc=example,dc=com", endpoint);
    endpoint = endpoints.get(1);
    assertEquals("ldaps://node2.example.com:636/dc=example,dc=com", endpoint);
  }

  @Test
  @Disabled("network access")
  void test() throws NamingException {
    Hashtable<String, String> env = new Hashtable<>();
    env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
    env.put(Context.PROVIDER_URL, "ldap:///dc=example,dc=com");
    DirContext ctx = new InitialDirContext(env);
  }

}
