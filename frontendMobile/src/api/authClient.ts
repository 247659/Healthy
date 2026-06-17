import {
  LoginRequest,
  PatientRegistrationRequest,
  Token,
} from '../types/auth.ts';
import { apiClient } from './apiClient.ts';


export const authService = {

  registerPatient: async (data: PatientRegistrationRequest): Promise<Token> => {
    const response = await apiClient.post<Token>(
      '/auth/register/patient',
      data,
    );
    return response.data;
  },

  login: async (data: LoginRequest): Promise<Token> => {
    const response = await apiClient.post<Token>(
      '/auth/login',
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
