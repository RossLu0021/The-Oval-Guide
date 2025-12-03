import LogoWordmark from "@/components/brand/LogoWordmark";
import PreauthNav from "@/components/mine/preauth-nav";
import SearchBox from "@/components/search/SearchBox";

export default function Home() {
  return (
    <div className="bg-background flex min-h-[100svh] flex-col">
      <header className="supports-[backdrop-filter]:bg-background/60 sticky top-0 z-30 pt-[max(env(safe-area-inset-top),0px)] backdrop-blur">
        <div className="mx-auto flex w-full max-w-7xl justify-end px-3 py-3 sm:px-6 sm:py-4 lg:px-8">
          <PreauthNav />
        </div>
      </header>

      <main className="flex-1 px-3 sm:px-6 lg:px-8">
        <div className="mx-auto flex max-w-3xl flex-col items-center">
          <div className="mt-[8svh] flex w-full flex-col items-center gap-6 sm:mt-[10svh] sm:gap-8">
            <LogoWordmark className="select-none" />
            <div className="w-full max-w-[680px] sm:max-w-2xl lg:max-w-3xl">
              <SearchBox />
            </div>
          </div>
        </div>
      </main>
    </div>
  );
}
