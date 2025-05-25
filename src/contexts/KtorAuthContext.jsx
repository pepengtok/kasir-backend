// src/contexts/KtorAuthContext.jsx
import React, { createContext, useState, useContext, useEffect } from "react";
import { ktorApi } from "../lib/ktorApi";

const KtorAuthContext = createContext({ user: null, login: async ()=>{}, logout: ()=>{} });

export function KtorAuthProvider({ children }) {
    const [user, setUser] = useState(null);

    // Saat mount, coba ambil profil kalau ada token
    useEffect(() => {
        const token = localStorage.getItem("ktor_token");
        if (!token) return;

        (async () => {
            try {
                const { data } = await ktorApi.get("/auth/me");
                setUser(data);  // data === { id, email, role }
            } catch {
                // misal token expiry → hapus
                localStorage.removeItem("ktor_token");
                setUser(null);
            }
        })();
    }, []);

    const login = async (email, password) => {
        const { data } = await ktorApi.post("/auth/login", { email, password });
        localStorage.setItem("ktor_token", data.token);
        setUser(data.user);
    };

    const logout = () => {
        localStorage.removeItem("ktor_token");
        setUser(null);
    };

    return (
        <KtorAuthContext.Provider value={{ user, login, logout }}>
            {children}
        </KtorAuthContext.Provider>
    );
}

export function useKtorAuth() {
    return useContext(KtorAuthContext);
}
