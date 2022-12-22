# [2.0.0](https://github.com/gravitee-io/gravitee-policy-xslt/compare/1.6.4...2.0.0) (2022-12-22)


### Bug Fixes

* disallow DocType declaration to prevent XML entity attacks ([c7ff66e](https://github.com/gravitee-io/gravitee-policy-xslt/commit/c7ff66e06f7b651d8fd6f08d67f7355cd060e1a9))
* put template in cache instead of recomputing it for each call ([fd02e99](https://github.com/gravitee-io/gravitee-policy-xslt/commit/fd02e99212ec916b97ab6211f43ecf2312b1e2b7))


### BREAKING CHANGES

* This policy now has a secure processing by default.
To keep the previous behavior you need to explicitly deactivate the secure processing.
To do so you need to set `policy.xslt.secure-processing: false` in your Gateway's `gravitee.yml`.

## [1.6.4](https://github.com/gravitee-io/gravitee-policy-xslt/compare/1.6.3...1.6.4) (2022-11-15)


### Bug Fixes

* disable access to external resources and functions for securization ([eaf081e](https://github.com/gravitee-io/gravitee-policy-xslt/commit/eaf081e734c7128f852239dd7321b65bbfc976ac))

## [1.6.3](https://github.com/gravitee-io/gravitee-policy-xslt/compare/1.6.2...1.6.3) (2022-09-08)


### Bug Fixes

* upgrade all gravitee dependencies ([490e629](https://github.com/gravitee-io/gravitee-policy-xslt/commit/490e629f93beef3d658ed00843feead4ceb25da5))

## [1.6.2](https://github.com/gravitee-io/gravitee-policy-xslt/compare/1.6.1...1.6.2) (2022-08-30)


### Bug Fixes

* do not cache spel evaluation result for parameters ([1a8d735](https://github.com/gravitee-io/gravitee-policy-xslt/commit/1a8d7359e7ceb009d16c0ab96e0d542804175997))

## [1.6.1](https://github.com/gravitee-io/gravitee-policy-xslt/compare/1.6.0...1.6.1) (2022-03-04)


### Bug Fixes

* use chain for TransformableStream to fail if TransformationException ([2aa3c12](https://github.com/gravitee-io/gravitee-policy-xslt/commit/2aa3c12c457946fb7ecf923d600279313e933e2e))
