import { NextRequest } from 'next/server';
export const dynamic = 'force-dynamic';
async function forward(request:NextRequest,{params}:{params:Promise<{path:string[]}>}) {
  const {path}=await params;
  const target=new URL(`/api/${path.join('/')}${request.nextUrl.search}`,process.env.API_INTERNAL_URL || 'http://localhost:8080');
  const headers=new Headers();
  for(const name of ['cookie','content-type','accept']){const value=request.headers.get(name);if(value)headers.set(name,value);}
  try{
    const upstream=await fetch(target,{method:request.method,headers,body:['GET','HEAD'].includes(request.method)?undefined:await request.arrayBuffer(),cache:'no-store'});
    const outputHeaders=new Headers();
    for(const name of ['content-type','set-cookie','cache-control']){const value=upstream.headers.get(name);if(value)outputHeaders.set(name,value);}
    return new Response(upstream.body,{status:upstream.status,headers:outputHeaders});
  }catch{return Response.json({message:'服务暂时不可用，请稍后重试'}, {status:503});}
}
export {forward as GET,forward as POST};
