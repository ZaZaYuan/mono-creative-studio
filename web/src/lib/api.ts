export type Template = { id:string;title:string;category:string;description:string;coverUrl:string;requiredImageCount:number;acceptedMimeTypes:string;maxFileSizeBytes:number };
export type Task = { id:string;ownerId:string;templateId:string;status:'PENDING'|'PROCESSING'|'SUCCEEDED'|'FAILED';currentStage:string|null;failureReason:string|null;resultFileId:string|null;createdAt:string;updatedAt:string };
const BASE='/api/backend';
export async function api<T>(path:string,init?:RequestInit):Promise<T> { const r=await fetch(BASE+path,{...init,cache:'no-store'}); if(!r.ok) { const data=await r.json().catch(()=>({message:'请求暂时不可用'})); throw new Error(data.message || '请求暂时不可用'); } return r.json(); }
export function createTask(form:FormData,onProgress:(percent:number|null)=>void,onUploaded:()=>void):Promise<Task>{
  return new Promise((resolve,reject)=>{
    const xhr=new XMLHttpRequest();
    xhr.open('POST',BASE+'/tasks');
    xhr.upload.onprogress=event=>onProgress(event.lengthComputable?Math.round(event.loaded/event.total*100):null);
    xhr.upload.onload=onUploaded;
    xhr.onerror=()=>reject(new Error('图片上传失败，请检查网络后重试'));
    xhr.onabort=()=>reject(new Error('图片上传已取消'));
    xhr.onload=()=>{
      let data:Task & {message?:string};
      try{data=JSON.parse(xhr.responseText);}catch{reject(new Error('提交响应无法读取，请到“我的创作”检查任务是否已创建'));return;}
      if(xhr.status===202)resolve(data);
      else reject(new Error(data.message||'提交失败，请到“我的创作”检查任务状态'));
    };
    xhr.send(form);
  });
}
export const fileUrl=(id:string)=>`${BASE}/files/${id}`;
export const stages=[{key:'MATERIAL_CHECK',label:'素材检查'},{key:'GENERATION',label:'模拟生成'},{key:'FINALIZATION',label:'结果整理'}];
