# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## Version 1.2.1 - XX.XX.2024
### Added
- #177 Tests für Repositories
### Changed
- #177 Falsche Konstanten in QueryUtil geändert

## Version 1.2.0 - 29.05.2024
### Added
- #199 - feat(flapdoodle): make download great again (on MEAP)
- #141 - Exception Handling Webclient
- #183 - new featureflag for processing neartime data
- #183 - get neartime data if present from acds
- #168 - context propagation

## Version 1.1.2 - 14.05.2024
- #193 - Update ACDS API for new enum values
- #178 - Update Kafka IT to start embedded Kafka once

## Version 1.1.1 - 07.05.2024
### Changed
- #191 - Parsing ignores unknown fields for event processing
- #95  - set Dev testing config to use separate mongodb service in cloud
### Fixed
- #191 - Event parsing errors

## Version 1.1.0 - 06.05.2024
### Added
- #162 Write feature flag for neartime data to database
- #185 fix Renovate automerge
### Changed
- #184 - Upgrade to Spring Boot 3.2.5

## Version 1.0.4 - 02.05.2024
### Added
- #154 MongoClient Bean Konfiguration
- #182 Project Level Codestyle settings
### Changed
- #64 update dependency flapdoodle.embed.mongo
- #164 Processing Service Test Coverage + Cleanup Teil 2

## Version 1.0.3 - 22.04.2024
### Added
- #146 Improved test coverage
- #175 Increase Memory-Allocation in qs and prod to 1024M auf 2048M
### Changed
- #157 Updated custom Splunk-Logging properties
- #172 Updated publish-API Jenkinsfile
- #126 Added Renovatebot schedule

## Version 1.0.2 - 17.04.2024
### Added
- #172 Use of Jenkins CnRZ (used to be the shared instanz on CRZ)
- #126 Renovatebot Configuration
### Changed
- #157 Updated custom Splunk-Logging properties

## Version 1.0.1 - 16.04.2024
### Changed
- re-release because of problems with git-flow

## Version 1.0.0 - 16.04.2024
### Added
- initial release MVP 