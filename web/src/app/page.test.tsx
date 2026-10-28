import { afterEach, describe, expect, it, vi } from 'vitest';
import { cleanup, fireEvent, render, screen, waitFor } from '@testing-library/react';
import Home from './page';

const template = { id:'pet-portrait',title:'毛茸茸的主角',category:'宠物故事',description:'宠物肖像',coverUrl:'/covers/pet.svg',requiredImageCount:1,acceptedMimeTypes:'image/png',maxFileSizeBytes:5242880 };
const task = (id:string,status:string,reason:string|null=null) => ({ id,ownerId:'owner',templateId:'pet-portrait',status,currentStage:'GENERATION',failureReason:reason,resultFileId:null,createdAt:'2026-10-28T12:00:00',updatedAt:'2026-10-28T12:00:00' });
const response = (data:unknown) => ({ok:true,json:async()=>data});

afterEach(()=>{cleanup();vi.unstubAllGlobals();});

describe('个人任务界面',()=>{
  it('选择图片后显示预览并允许提交',async()=>{
    vi.stubGlobal('fetch',vi.fn(async(input:string)=>input.includes('/templates')?response([template]):response([])));
    vi.stubGlobal('URL',class extends URL {static createObjectURL(){return 'blob:demo'} static revokeObjectURL(){}});
    render(<Home/>);
    fireEvent.click(await screen.findByRole('button',{name:/毛茸茸的主角/}));
    const file=new File(['image'],'demo.png',{type:'image/png'});
    fireEvent.change(screen.getByLabelText('上传第 1 张图片'),{target:{files:[file]}});
    await waitFor(()=>expect(screen.getByText('1 / 1')).toBeTruthy());
    expect((screen.getByRole('button',{name:/开始创作/}) as HTMLButtonElement).disabled).toBe(false);
  });

  it('同时保留两个任务各自的状态与错误',async()=>{
    vi.stubGlobal('fetch',vi.fn(async(input:string)=>input.includes('/templates')?response([template]):response([task('task-a','PROCESSING'),task('task-b','FAILED','模拟生成阶段失败')])));
    render(<Home/>);
    await waitFor(()=>expect(screen.getByRole('button',{name:/我的创作/})).toBeTruthy());
    fireEvent.click(screen.getByRole('button',{name:/我的创作/}));
    await waitFor(()=>expect(screen.getByText('正在创作')).toBeTruthy());
    expect(screen.getByText('创作失败')).toBeTruthy();
    expect(screen.getByText('模拟生成阶段失败')).toBeTruthy();
  });

  it('查询暂时不可用时保留后台任务状态',async()=>{
    let historyCalls=0;
    vi.stubGlobal('fetch',vi.fn(async(input:string)=>{
      if(input.includes('/templates'))return response([template]);
      historyCalls++;
      if(historyCalls>1)throw new Error('network unavailable');
      return response([task('task-a','PROCESSING')]);
    }));
    render(<Home/>);
    await waitFor(()=>expect(screen.getByRole('button',{name:/我的创作/})).toBeTruthy());
    fireEvent.click(screen.getByRole('button',{name:/我的创作/}));
    await waitFor(()=>expect(screen.getByText(/暂时无法获取最新状态/)).toBeTruthy());
    expect(screen.getByText('正在创作')).toBeTruthy();
    expect(screen.queryByText('创作失败')).toBeNull();
  });
});
