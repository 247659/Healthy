import type {
    LoginRequest,
    Token,
} from '../types/auth.ts';
import {apiNoAuth} from './api.ts';


export const authService = {

    login: async (data: LoginRequest): Promise<Token> => {
        const response = await apiNoAuth.post<Token>(
            '/auth/loginDoctor',
            data,
        );
        return response.data;
    },

    logout: async (data: string | null) => {
        const response = await apiNoAuth.post(
            '/auth/logout',
            {
                refreshToken: data
            },
        )
        return response.data;
    },

    refreshToken: async (refreshToken: string | null): Promise<Token> => {
        const response = await apiNoAuth.post<Token>(
            '/auth/refresh',
            {
                refreshToken: refreshToken,
            },
        );
        return response.data;
    },

};
