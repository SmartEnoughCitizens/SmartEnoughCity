export function safeJsonParse<T>(
  jsonString: string,
  schema: { parse: (data: unknown) => T },
): T {
  // eslint-disable-next-line no-restricted-syntax
  const raw = JSON.parse(jsonString);
  return schema.parse(raw);
}
