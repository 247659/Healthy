import { StyleSheet } from 'react-native';

export const globalStyles = StyleSheet.create({
  errorContainer: {
    backgroundColor: '#FEE2E2',
    padding: 12,
    borderRadius: 12,
    marginBottom: 16,
    borderWidth: 1,
    borderColor: '#FCA5A5',
  },
  errorText: {
    color: '#EF4444',
    fontSize: 14,
    textAlign: 'center',
    fontWeight: '500',
  },
  successContainer: {
    backgroundColor: '#D1FAE5',
    padding: 12,
    borderRadius: 12,
    marginBottom: 16,
    borderWidth: 1,
    borderColor: '#6EE7B7',
  },
  successText: {
    color: '#065F46',
    fontSize: 14,
    textAlign: 'center',
    fontWeight: '600',
  },

  inputContainer: { gap: 16, marginBottom: 32 },
  inputLabel: {
    fontSize: 14,
    fontWeight: '600',
    color: '#4B5563',
    marginBottom: 6,
    marginLeft: 4,
  },
  input: {
    backgroundColor: '#F3F4F6',
    borderRadius: 16,
    padding: 18,
    fontSize: 16,
    color: '#1F2937',
  },
});
