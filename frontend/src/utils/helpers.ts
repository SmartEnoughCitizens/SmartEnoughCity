import { User, UserRole } from '../types';

// Format date to readable string
export const formatDate = (date: string | Date | null | undefined): string => {
  if (!date) return '';
  return new Date(date).toLocaleDateString('en-US', {
    year: 'numeric',
    month: 'long',
    day: 'numeric',
  });
};

// Format date and time
export const formatDateTime = (date: string | Date | null | undefined): string => {
  if (!date) return '';
  return new Date(date).toLocaleString('en-US', {
    year: 'numeric',
    month: 'short',
    day: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  });
};

// Format number with commas
export const formatNumber = (num: number | null | undefined): string => {
  if (num === null || num === undefined) return '0';
  return num.toLocaleString();
};

// Format percentage
export const formatPercentage = (value: number | null | undefined): string => {
  if (value === null || value === undefined) return '0%';
  return `${value.toFixed(1)}%`;
};

// Truncate text
export const truncateText = (text: string | null | undefined, maxLength: number): string => {
  if (!text || text.length <= maxLength) return text || '';
  return `${text.substring(0, maxLength)}...`;
};

// Check if user has role
export const hasRole = (user: User | null | undefined, role: UserRole | UserRole[]): boolean => {
  if (!user || !user.role) return false;
  if (Array.isArray(role)) {
    return role.includes(user.role);
  }
  return user.role === role;
};

// Get role display name
export const getRoleDisplayName = (role: UserRole | string): string => {
  const roleMap: Record<string, string> = {
    GOVERNMENT_ADMIN: 'Government Admin',
    CITY_MANAGER: 'City Manager',
    SERVICE_PROVIDER_ADMIN: 'Service Provider Admin',
    SERVICE_PROVIDER_USER: 'Service Provider User',
  };
  return roleMap[role] || role;
};

// Generate random color for charts
export const getRandomColor = (): string => {
  const letters = '0123456789ABCDEF';
  let color = '#';
  for (let i = 0; i < 6; i++) {
    color += letters[Math.floor(Math.random() * 16)];
  }
  return color;
};

// Debounce function
export const debounce = <T extends (...args: unknown[]) => void>(
  func: T,
  wait: number
): ((...args: Parameters<T>) => void) => {
  let timeout: NodeJS.Timeout | null = null;
  return function executedFunction(...args: Parameters<T>) {
    const later = () => {
      timeout = null;
      func(...args);
    };
    if (timeout) {
      clearTimeout(timeout);
    }
    timeout = setTimeout(later, wait);
  };
};
