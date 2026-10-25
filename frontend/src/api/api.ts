import { Xior } from "xior";

const baseURL = "http://localhost:8080/api"; 

export const http = Xior.create({
    baseURL,
    credentials: "include"
});

