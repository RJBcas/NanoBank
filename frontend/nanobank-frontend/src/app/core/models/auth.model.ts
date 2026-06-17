export interface LoginRequest {
  email: string;
  password: string;
}

export interface RegisterRequest {
  email: string;
  password: string;
  fullName: string;
}

export interface AuthResponse {
  accessToken: string;
  tokenType: string;
  userId: string;
  email: string;
  fullName: string;
  role: string;
}
