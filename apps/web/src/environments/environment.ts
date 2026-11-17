// Production build config. Everything goes through the Gateway now (README §4) -
// no service port is called directly from the browser.
export const environment = {
  production: true,
  gatewayUrl: 'http://localhost:8085',
  keycloak: {
    issuer: 'http://localhost:8180/realms/supplymind',
    clientId: 'web',
  },
};
