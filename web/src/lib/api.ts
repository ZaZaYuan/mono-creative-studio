export type Template = { id:string;title:string;category:string;description:string;coverUrl:string;requiredImageCount:number;acceptedMimeTypes:string;maxFileSizeBytes:number };
export type Task = { id:string;ownerId:string;templateId:string;status:'PENDING'|'PROCESSING'|'SUCCEEDED'|'FAILED';currentStage:string|null;failureReason:string|null;resultFileId:string|null;createdAt:string;updatedAt:string };
const BASE='/api/backend';
export async function api<T>(path:string,init?:RequestInit):Promise<T> { const r=await fetch(BASE+path,{...init,cache:'no-store'}); if(!r.ok) { const data=await r.json().catch(()=>({message:'请求暂时不可用'})); throw new Error(data.message || '请求暂时不可用'); } return r.json(); }
export const fileUrl=(id:string)=>`${BASE}/files/${id}`;
export const stages=[{key:'MATERIAL_CHECK',label:'素材检查'},{key:'GENERATION',label:'模拟生成'},{key:'FINALIZATION',label:'结果整理'}];
