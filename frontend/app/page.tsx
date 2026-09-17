import { redirect } from 'next/navigation'

export default function Home() {
    // 공개 데모는 빈 DB에 처음 적재한 합성 경기로 연결합니다.
    redirect(process.env.NEXT_PUBLIC_DEMO_MODE === 'true' ? '/matches/1' : '/matches/0')
}
