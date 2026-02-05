import { redirect } from 'next/navigation'

export default function Home() {
    // 접속하자마자 '/matches/0'으로 강제 이동시킵니다.
    redirect('/matches/0')
}