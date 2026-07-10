# Contributing to DataBuff

DataBuff is an open-source AI-native OpenTelemetry APM platform, and we welcome contributions from everyone. Whether you're fixing a bug, adding a feature, improving documentation, or helping others in the community, your involvement is appreciated.

## Table of Contents

- [Code of Conduct](#code-of-conduct)
- [Getting Started](#getting-started)
- [Development Environment](#development-environment)
- [How to Contribute](#how-to-contribute)
  - [Report a Bug](#report-a-bug)
  - [Request a Feature](#request-a-feature)
  - [Submit a Pull Request](#submit-a-pull-request)
- [Pull Request Guidelines](#pull-request-guidelines)
- [Commit Message Convention](#commit-message-convention)
- [Style Guides](#style-guides)
- [Community](#community)

## Code of Conduct

This project follows a Contributor Covenant Code of Conduct. By participating, you are expected to uphold this code. Please report unacceptable behavior to the community maintainers via WeChat group (see README).

## Getting Started

1. **Fork** the repository on GitHub.
2. **Clone** your fork:
   ```bash
   git clone https://github.com/databufflabs/databuff.git
   cd databuff
   ```
3. **Set up** the development environment (see below).
4. **Create a branch** for your changes:
   ```bash
   git checkout -b feat/your-feature-name
   ```

## Development Environment

DataBuff uses a Docker Compose-based development stack. The simplest way to get started:

```bash
# Start all services
docker compose -f deploy/local/docker-compose.yml up -d

# Open http://localhost:27403
# Default login: admin / Databuff@123
```

For detailed instructions, see [deploy/README.md](deploy/README.md).

### Prerequisites

- Java 17+ (for backend development)
- Node.js 18+ (for frontend development)
- Docker & Docker Compose (for local environment)

## How to Contribute

### Report a Bug

Use the [Bug Report template](.github/ISSUE_TEMPLATE/bug_report.md) to submit a bug. Please include:

- A clear description of the issue
- Steps to reproduce
- Expected vs actual behavior
- Environment details (OS, browser, DataBuff version)
- Screenshots or logs if relevant

### Request a Feature

Use the [Feature Request template](.github/ISSUE_TEMPLATE/feature_request.md). Describe:

- The problem you're trying to solve
- Your proposed solution
- Any alternatives you've considered

### Submit a Pull Request

1. **Keep PRs focused** — one logical change per PR. Large changes should be broken into smaller, reviewable units.
2. **Update documentation** if your change affects public APIs, configuration, or installation steps.
3. **Add tests** for new functionality. Existing tests must continue to pass.
4. **Sign the DCO** — all commits must include a `Signed-off-by` line (`git commit -s`).
5. **Link related issues** in your PR description.

## Pull Request Guidelines

- **Title**: Use the [Conventional Commits](#commit-message-convention) format.
- **Description**: Explain what the PR does, why, and how to test it. Reference any related issues.
- **Size**: Keep PRs under ~400 lines when possible. Large refactors should be discussed in an issue first.
- **Review**: At least one maintainer review is required before merging.
- **CI**: All checks must pass before merge.

## Commit Message Convention

We follow [Conventional Commits](https://www.conventionalcommits.org/):

```
<type>(<scope>): <description>

[optional body]
[optional footer(s)]
```

Types: `feat`, `fix`, `docs`, `style`, `refactor`, `perf`, `test`, `chore`, `ci`

Examples:
```
feat(ingest): add OTLP log ingestion support
fix(web): correct trace query pagination offset
docs: update deployment guide for v0.2.0
```

## Style Guides

### Java

- Follow [Google Java Style](https://google.github.io/styleguide/javaguide.html)
- Use 4 spaces for indentation (not tabs)
- Keep methods focused and under 50 lines when possible

### TypeScript / Vue

- Use 2 spaces for indentation
- Follow the existing patterns in `ai-apm-frontend/src/`
- Use TypeScript types for all new code

### Documentation

- Write in clear, concise English (or Chinese in the Chinese docs)
- Include code examples for commands and API usage
- Keep line length under 120 characters

## Community

- **WeChat**: Scan the QR code in README to join the community group
- **GitHub Discussions**: Use [Discussions](https://github.com/databufflabs/databuff/discussions) for Q&A and ideas
- **Good First Issues**: Browse [good first issues](https://github.com/databufflabs/databuff/labels/good%20first%20issue) for beginner-friendly tasks
- **Issue tracking**: Report bugs and request features via [GitHub Issues](https://github.com/databufflabs/databuff/issues)

Thank you for contributing to DataBuff!
