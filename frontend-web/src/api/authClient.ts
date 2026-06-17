import type {
  LoginRequest,
  Token,
} from '../types/auth.ts';
import { apiClient } from './apiClient.ts';


export const authService = {

  login: async (data: LoginRequest): Promise<Token> => {
    const response = await apiClient.post<Token>(
      '/auth/loginDoctor',
      data,
    );
    return response.data;
  },

  logout: async (data: string | null) => {
     const response = await apiClient.post(
       '/auth/logout',
       {
         refreshToken : data
       },
     )
    return response.data;
  }

};
