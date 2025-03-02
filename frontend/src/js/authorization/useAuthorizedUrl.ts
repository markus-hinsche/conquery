import { useContext } from "react";

import { AuthTokenContext } from "./AuthTokenProvider";

export const useGetAuthorizedUrl = () => {
  const { authToken } = useContext(AuthTokenContext);

  const encodedAuthToken = encodeURIComponent(authToken);

  return (url: string) =>
    `${url}?access_token=${encodedAuthToken}&charset=utf-8&pretty=false`;
};
