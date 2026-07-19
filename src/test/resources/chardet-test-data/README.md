# chardet test data

Test data files for the [chardet](https://github.com/chardet/chardet) Universal Encoding Detector. Each file is organized into directories named `{encoding}` or `{encoding}-{language}` (e.g., `big5-chinese`, `utf-8-english`, `cp037-breton`).

See [CATALOG.md](CATALOG.md) for a full listing of every file's provenance and characteristics.

## Data quality

Run [`scripts/check_test_data.py`](scripts/check_test_data.py) to verify that all files decode correctly with their labeled encoding and pass quality checks (mojibake, control characters, language/script mismatches):

```bash
python3 scripts/check_test_data.py .
```

## Contributing

Contributions of openly-licensed test data are welcome at <https://github.com/chardet/chardet>.

## License

Each test file is copyright its respective publisher.
